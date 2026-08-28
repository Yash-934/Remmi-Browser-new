package com.netrunner.adblock

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.mozilla.geckoview.WebExtension

/**
 * Native Messaging Delegate for Remmi GeckoView WebExtension.
 * Dedicated to threat neutralization, tracker blocking, and click transparency DOM inspection.
 *
 * CRITICAL ARCHITECTURAL INVARIANT:
 * WebExtension proxy authority has been completely removed.
 * Native Gecko layer (GeckoRuntime / GeckoSession) is the SOLE authoritative manager of proxy
 * routing, Tor SOCKS5 isolation, and network hardening.
 */
enum class ExtensionState {
  NOT_REGISTERED,
  REGISTERED,
  CONNECTED,
  DISCONNECTED,
  FAILED
}

class BlockExtension private constructor(private val adblockBridge: AdblockBridge) : WebExtension.MessageDelegate {

  var onThreatNeutralized: ((url: String, type: String) -> Unit)? = null
  var onHtmlExtracted: ((url: String, html: String) -> Unit)? = null
  var onClickInspected: ((candidates: List<JSONObject>, hasOverlay: Boolean, intercepted: Boolean, pageUrl: String) -> Unit)? = null

  private val _extensionState = MutableStateFlow(ExtensionState.NOT_REGISTERED)
  val extensionState: StateFlow<ExtensionState> = _extensionState.asStateFlow()

  @Volatile
  private var activePort: WebExtension.Port? = null

  fun setExtensionRegistered() {
    if (_extensionState.value == ExtensionState.NOT_REGISTERED) {
      _extensionState.value = ExtensionState.REGISTERED
    }
  }

  fun setExtensionFailed(reason: String) {
    _extensionState.value = ExtensionState.FAILED
    log("[WEBEXT] Extension registration failed: $reason")
  }

  override fun onConnect(port: WebExtension.Port) {
    log("[WEBEXT] Native port connected: ${port.name}")
    activePort = port
    _extensionState.value = ExtensionState.CONNECTED

    port.setDelegate(object : WebExtension.PortDelegate {
      override fun onPortMessage(message: Any, p: WebExtension.Port) {
        if (message is JSONObject) {
          val type = message.optString("type").ifEmpty { message.optString("action") }
          val url = message.optString("url")
          val category = message.optString("category").ifEmpty { message.optString("type", "script") }
          val msgText = message.optString("message").ifEmpty { message.optString("msg") }
          val status = message.optString("status")

          when (type) {
            "PORT_STATUS" -> {
              val role = message.optString("role", "AD_TRACKER_BLOCKER_ONLY")
              log("[WEBEXT] Port status: $status (role=$role)")
            }
            "CLICK_INSPECTION_RESULT" -> {
              val candidatesArray = message.optJSONArray("candidates")
              val hasOverlay = message.optBoolean("hasOverlay", false)
              val intercepted = message.optBoolean("intercepted", false)
              val pageUrl = message.optString("pageUrl", "")
              val candidatesList = mutableListOf<JSONObject>()
              if (candidatesArray != null) {
                for (i in 0 until candidatesArray.length()) {
                  val c = candidatesArray.optJSONObject(i)
                  if (c != null) candidatesList.add(c)
                }
              }
              log("[WEBEXT] Click inspection received: ${candidatesList.size} candidates (hasOverlay=$hasOverlay, intercepted=$intercepted)")
              onClickInspected?.invoke(candidatesList, hasOverlay, intercepted, pageUrl)
            }
            "BLOCKED", "blocked" -> {
              if (url.isNotEmpty()) {
                log("[TRACKER] Neutralized: $url ($category)")
                adblockBridge.totalBlockedCount.incrementAndGet()
                onThreatNeutralized?.invoke(url, category)
              }
            }
            "EXTRACTED_HTML", "extracted_html" -> {
              val html = message.optString("html")
              onHtmlExtracted?.invoke(url, html)
            }
            "LOG", "log" -> {
              if (msgText.isNotEmpty()) {
                log(msgText)
              }
            }
            else -> {
              log("[WEBEXT] Raw message received: $message")
            }
          }
        } else {
          log("[WEBEXT] Non-JSON message received: $message")
        }
      }

      override fun onDisconnect(p: WebExtension.Port) {
        log("[WEBEXT] Native port disconnected")
        if (activePort == p) {
          activePort = null
          _extensionState.value = ExtensionState.DISCONNECTED
        }
      }
    })
  }

  fun extractActiveTabHtml() {
    val msg = JSONObject().apply {
      put("type", "EXTRACT_HTML")
      put("action", "extract_html")
    }
    val currentPort = activePort
    if (currentPort != null) {
      try {
        currentPort.postMessage(msg)
      } catch (e: Exception) {
        log("[WEBEXT] Could not send extract_html: ${e.message}")
      }
    }
  }

  companion object {
    private const val TAG = "BlockExtension"

    fun log(message: String) {
      com.remmi.browser.util.DebugLogManager.log(message)
    }

    val debugLogs: StateFlow<List<String>> = com.remmi.browser.util.DebugLogManager.logs

    fun clearLogs() {
      com.remmi.browser.util.DebugLogManager.clear()
    }

    @Volatile
    private var INSTANCE: BlockExtension? = null

    fun getInstance(bridge: AdblockBridge = AdblockBridge.getInstance()): BlockExtension {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: BlockExtension(bridge).also { INSTANCE = it }
      }
    }
  }
}
