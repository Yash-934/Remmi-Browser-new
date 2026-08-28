package com.remmi.browser.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Single source of truth for the active Tor route in Remmi Browser.
 * Prevents hardcoding of SOCKS ports across the application.
 */
data class TorRouteInfo(
  val host: String = "127.0.0.1",
  val socksPort: Int? = null,
  val isGhostActive: Boolean = false,
  val isVerified: Boolean = false,
  val exitIp: String? = null,
  val failoverDirect: Boolean = false,
  val generation: Long = 0L,
)

object CurrentTorRoute {
  private val generationSequence = AtomicLong(1L)
  private val _route = MutableStateFlow(TorRouteInfo(socksPort = null, isGhostActive = false, generation = 0L))
  val route: StateFlow<TorRouteInfo> = _route.asStateFlow()

  val currentSocksPort: Int?
    get() = _route.value.socksPort

  val isGhostActive: Boolean
    get() = _route.value.isGhostActive

  val isVerified: Boolean
    get() = _route.value.isVerified

  val exitIp: String?
    get() = _route.value.exitIp

  val currentGeneration: Long
    get() = _route.value.generation

  val isReady: Boolean
    get() = _route.value.socksPort != null && (_route.value.socksPort ?: 0) > 0 && _route.value.isGhostActive

  fun markStartingGhost(): Long {
    val generation = generationSequence.incrementAndGet()
    _route.value = _route.value.copy(
      generation = generation,
      // We don't clear socksPort here in case it's still valid from a previous session,
      // but we update the generation so that any pending asynchronous Shield clears are invalidated.
      failoverDirect = false
    )
    return generation
  }

  fun updateRoute(
    socksPort: Int?,
    isGhostActive: Boolean,
    isVerified: Boolean = false,
    exitIp: String? = null,
    generation: Long = generationSequence.incrementAndGet(),
  ): Long {
    _route.value = TorRouteInfo(
      host = "127.0.0.1",
      socksPort = socksPort,
      isGhostActive = isGhostActive,
      isVerified = isVerified,
      exitIp = exitIp,
      failoverDirect = false,
      generation = generation,
    )
    return generation
  }

  fun clearRoute(generation: Long = generationSequence.incrementAndGet()): Long {
    _route.value = TorRouteInfo(
      host = "127.0.0.1",
      socksPort = null,
      isGhostActive = false,
      isVerified = false,
      exitIp = null,
      failoverDirect = true,
      generation = generation,
    )
    return generation
  }
}

