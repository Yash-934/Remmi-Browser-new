package com.netrunner.adblock

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

/**
 * NetRunner Adblock Bridge
 * Bridges to native Rust adblock engine (libadblock_rust.so) with fallback to built-in rules.
 */
class AdblockBridge {

  private val mutex = Mutex()
  private val blockedHostnames = mutableSetOf<String>()
  private val blockedSubstrings = mutableListOf<String>()
  private val allowList = mutableSetOf<String>()

  val totalBlockedCount = AtomicInteger(0)
  var isNativeLoaded: Boolean = false
    private set
  
  private var enginePtr: Long = 0L

  init {
    initEngine()
  }

  private fun initEngine() {
    try {
      System.loadLibrary("adblock_rust")
      // Attempt to create engine, handle both static (void/boolean) and instance (Long ptr) returns if possible.
      // Assuming it returns a Long pointer, or 0 if it failed/uses global state.
      enginePtr = try { nativeCreateEngine() } catch (e: NoSuchMethodError) { 
          // If the signature expects arguments, we fallback
          0L 
      }
      isNativeLoaded = true
      Log.i(TAG, "Native adblock_rust loaded successfully! EnginePtr: $enginePtr")
    } catch (e: UnsatisfiedLinkError) {
      Log.w(TAG, "libadblock_rust.so not found or signature mismatch. Using Kotlin fallback engine.", e)
      isNativeLoaded = false
    } catch (e: Throwable) {
      Log.w(TAG, "Failed initializing native adblock engine, falling back to Kotlin engine", e)
      isNativeLoaded = false
    }

    loadDefaultTrackerRules()
  }

  fun loadDefaultTrackerRules() {
    val defaultDomains = listOf(
      "doubleclick.net", "googlesyndication.com", "google-analytics.com",
      "googletagmanager.com", "adservice.google.com", "admob.com",
      "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net",
      "outbrain.com", "taboola.com", "scorecardresearch.com",
      "quantserve.com", "quantcount.com", "moatads.com",
      "pubmatic.com", "rubiconproject.com", "openx.net",
      "casalemedia.com", "applovin.com", "unityads.unity3d.com",
      "vungle.com", "appsflyer.com", "branch.io", "adjust.com",
      "kochava.com", "singular.net", "facebook.net/tr",
      "connect.facebook.net", "ads-twitter.com", "analytics.twitter.com",
      "bat.bing.com", "clarity.ms", "hotjar.com", "mouseflow.com",
      "segment.io", "segment.com", "mixpanel.com", "amplitude.com",
      "newrelic.com", "optimizely.com", "smartadserver.com",
      "yieldmo.com", "indexww.com", "chartbeat.com", "adroll.com",
      "advertising.com", "amazon-adsystem.com", "bidswitch.net",
      "revcontent.com", "mgid.com", "zergnet.com", "popads.net"
    )

    blockedHostnames.addAll(defaultDomains)

    val defaultPatterns = listOf(
      "/ads/", "/ad-banner", "/advertisement", "/trackers/",
      "pixel.gif", "beacon.js", "analytics.js", "gtag/js",
      "pagead2.googlesyndication.com", "adserver.", "adsystem.",
      "telemetry.", "tracking.", "statcounter.com"
    )
    blockedSubstrings.addAll(defaultPatterns)
  }

  suspend fun addCustomRule(rule: String) = mutex.withLock {
    val trimmed = rule.trim()
    if (trimmed.startsWith("@@")) {
      allowList.add(trimmed.removePrefix("@@").removePrefix("||"))
    } else if (trimmed.startsWith("||")) {
      blockedHostnames.add(trimmed.removePrefix("||").removeSuffix("^"))
    } else {
      blockedSubstrings.add(trimmed)
    }
  }

  suspend fun shouldBlock(url: String, sourceUrl: String = "", resourceType: String = "other"): Boolean = mutex.withLock {
    if (isNativeLoaded) {
      try {
        val blocked = if (enginePtr != 0L) {
          nativeMatches(enginePtr, url, sourceUrl, resourceType)
        } else {
          // Fallback if the JNI function signature expects no enginePtr (global state)
          // We will catch NoSuchMethodError below if this isn't right either
          try {
             val clazz = this::class.java
             val method = clazz.getDeclaredMethod("nativeMatches", String::class.java, String::class.java, String::class.java)
             method.isAccessible = true
             method.invoke(this, url, sourceUrl, resourceType) as Boolean
          } catch(e: Exception) {
             nativeMatches(0L, url, sourceUrl, resourceType) 
          }
        }
        
        if (blocked) {
          totalBlockedCount.incrementAndGet()
          return true
        }
        return false
      } catch (e: Throwable) {
        Log.e(TAG, "Native check URL failed, checking fallback", e)
      }
    }

    try {
      val uri = URI(url)
      val host = uri.host?.lowercase() ?: return false

      if (allowList.any { rule -> host == rule || host.endsWith(".$rule") }) {
        return false
      }

      for (blockedHost in blockedHostnames) {
        if (host == blockedHost || host.endsWith(".$blockedHost")) {
          totalBlockedCount.incrementAndGet()
          return true
        }
      }

      val lowerUrl = url.lowercase()
      for (pattern in blockedSubstrings) {
        if (lowerUrl.contains(pattern)) {
          totalBlockedCount.incrementAndGet()
          return true
        }
      }

      return false
    } catch (e: Exception) {
      return false
    }
  }

  fun getLoadedRulesCount(): Int {
    return blockedHostnames.size + blockedSubstrings.size
  }

  protected fun finalize() {
    if (isNativeLoaded && enginePtr != 0L) {
      try {
        nativeFreeEngine(enginePtr)
      } catch (e: Throwable) {
        // Ignored
      }
    }
  }

  // Native JNI functions implemented in rust/src/lib.rs
  private external fun nativeCreateEngine(): Long
  private external fun nativeMatches(enginePtr: Long, url: String, sourceUrl: String, requestType: String): Boolean
  private external fun nativeFreeEngine(enginePtr: Long)

  companion object {
    private const val TAG = "AdblockBridge"

    @Volatile
    private var INSTANCE: AdblockBridge? = null

    fun getInstance(): AdblockBridge {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: AdblockBridge().also { INSTANCE = it }
      }
    }
  }
}
