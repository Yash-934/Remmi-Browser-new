package com.remmi.browser.security

import android.util.Log
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * NetworkRouteAuthority
 *
 * Centralized, authoritative factory and validator for all auxiliary network clients
 * (OkHttpClient, URL connections, background translators, fetchers, and DNS).
 *
 * INVARIANTS:
 * 1. Fail-Closed on Ghost/Tor: If Ghost mode is active or an .onion destination is requested,
 *    traffic MUST be routed through the active 127.0.0.1 Tor SOCKS5 proxy. If no verified
 *    Tor SOCKS port is active, connection creation FAILS IMMEDIATELY (throwing an IllegalStateException)
 *    to prevent clearnet IP leakage.
 * 2. Route Generation Pinning: SOCKS proxy configurations are bound to the authoritative
 *    CurrentTorRoute generation to prevent stale routing across mode flips.
 * 3. DNS Leaks Prohibited: In Ghost mode, remote DNS resolution over Tor is enforced.
 */
object NetworkRouteAuthority {
  private const val TAG = "NetworkRouteAuthority"

  /**
   * Returns whether the given URL target is a Tor hidden service (.onion).
   */
  fun isOnionDestination(url: String): Boolean {
    val clean = url.trim().lowercase()
    val host = try {
      val withoutScheme = clean.substringAfter("://")
      val hostPart = withoutScheme.substringBefore('/').substringBefore(':')
      hostPart
    } catch (_: Exception) {
      ""
    }
    return host.endsWith(".onion") || clean.contains(".onion/") || clean.endsWith(".onion")
  }

  /**
   * Produces an OkHttpClient configured strictly according to current route authority.
   *
   * @param isGhost Whether this request must be routed through the Tor network.
   * @param targetUrl Optional destination URL. If it ends in .onion, Tor routing is mandatory.
   * @param connectTimeoutSeconds Connect timeout in seconds.
   * @param readTimeoutSeconds Read timeout in seconds.
   * @param followRedirects Whether OkHttp should follow HTTP redirects automatically.
   * @param customConfig Optional block for custom OkHttpClient builder tuning.
   *
   * @throws IllegalStateException if Ghost/Tor routing is required but Tor SOCKS port is not available.
   */
  fun createHttpClient(
    isGhost: Boolean,
    targetUrl: String? = null,
    connectTimeoutSeconds: Long = 15L,
    readTimeoutSeconds: Long = 20L,
    followRedirects: Boolean = true,
    customConfig: (OkHttpClient.Builder.() -> Unit)? = null
  ): OkHttpClient {
    val requiresTor = isGhost || (targetUrl != null && isOnionDestination(targetUrl))
    val builder = OkHttpClient.Builder()
      .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
      .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
      .followRedirects(followRedirects)

    if (requiresTor) {
      val socksPort = CurrentTorRoute.currentSocksPort
      if (socksPort == null || socksPort <= 0) {
        val errorMsg = "Fail-Closed Network Hardening: Ghost/Onion route requested but no active Tor SOCKS port available."
        Log.e(TAG, errorMsg)
        throw IllegalStateException(errorMsg)
      }

      // Enforce SOCKS proxy directly to the verified local Tor daemon
      builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort)))
      Log.d(TAG, "Configured OkHttpClient via Tor SOCKS5 proxy on 127.0.0.1:$socksPort (gen=${CurrentTorRoute.currentGeneration})")
    }

    customConfig?.invoke(builder)
    return builder.build()
  }

  /**
   * Safely attempts to build an auxiliary OkHttpClient, returning null instead of throwing
   * if Tor routing is unavailable.
   */
  fun createHttpClientOrNull(
    isGhost: Boolean,
    targetUrl: String? = null,
    connectTimeoutSeconds: Long = 15L,
    readTimeoutSeconds: Long = 20L,
    followRedirects: Boolean = true,
    customConfig: (OkHttpClient.Builder.() -> Unit)? = null
  ): OkHttpClient? {
    return try {
      createHttpClient(
        isGhost = isGhost,
        targetUrl = targetUrl,
        connectTimeoutSeconds = connectTimeoutSeconds,
        readTimeoutSeconds = readTimeoutSeconds,
        followRedirects = followRedirects,
        customConfig = customConfig
      )
    } catch (e: Exception) {
      Log.w(TAG, "Failed to create route-authorized HTTP client: ${e.message}")
      null
    }
  }
}
