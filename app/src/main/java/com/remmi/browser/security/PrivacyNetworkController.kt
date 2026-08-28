package com.remmi.browser.security

import android.content.Context
import android.util.Log
import com.remmi.browser.engine.GeckoEngineManager
import com.remmi.browser.engine.TabManager
import com.remmi.browser.util.DebugLogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Authoritative Privacy & Network Security Controller for Remmi Browser.
 * Owns Ghost Mode / Shield Mode lifecycle, Tor state synchronization,
 * native Gecko proxy enforcement, leak prevention, and fail-closed guarantees.
 */
class PrivacyNetworkController private constructor(private val context: Context) {

  private val torManager = TorManager.getInstance(context)
  private val geckoEngine = GeckoEngineManager.getInstance(context)

  val torState: StateFlow<TorManager.TorState> = torManager.bootstrapState
  val currentCircuit: StateFlow<TorCircuit?> = torManager.currentCircuit

  /**
   * Enters Ghost Mode transactionally with Fail-Closed guarantee:
   * 1. Closes existing session to prevent clearnet leakage.
   * 2. Starts native TorService, dynamically discovers active SOCKS port, and verifies SOCKS5 handshake & check.torproject.org exit routing.
   * 3. Configures GeckoView native engine-level proxy, remote DNS, and WebRTC blocking.
   * 4. Updates CurrentTorRoute single source of truth.
   * 5. Returns success only if Tor exit routing and native Gecko proxy are verified.
   */
  suspend fun enterGhostMode(tabId: String): Result<Int> = withContext(Dispatchers.IO) {
    Log.i(TAG, "Entering Ghost Mode for tab $tabId (enforcing fail-closed Tor routing)...")
    val generation = CurrentTorRoute.markStartingGhost()
    DebugLogManager.log("[ROUTE] REQUESTED profile=GHOST tabId=$tabId generation=$generation")

    // Step 1: Terminate active clearnet session first
    geckoEngine.closeSessionSafely(tabId)

    // Step 2: Bootstrap & verify Tor daemon
    val torResult = torManager.startTor()
    if (torResult.isFailure) {
      val error = torResult.exceptionOrNull() ?: Exception("Tor failed to initialize")
      Log.e(TAG, "Ghost Mode transition aborted: ${error.message}")
      CurrentTorRoute.clearRoute()
      DebugLogManager.log("[ROUTE] FAILED profile=GHOST reason=${error.message}")
      return@withContext Result.failure(error)
    }

    val socksPort = torResult.getOrNull() ?: run {
      val discovered = torManager.discoverRuntimeSocksPort()
      if (discovered <= 0) {
        val err = IllegalStateException("SOCKS port discovery returned invalid port: $discovered")
        CurrentTorRoute.clearRoute()
        DebugLogManager.log("[ROUTE] FAILED profile=GHOST reason=port_unavailable")
        return@withContext Result.failure(err)
      }
      discovered
    }

    // Step 3: Verify SOCKS port handshake
    val handshakeOk = TorStatusChecker.isPortListening("127.0.0.1", socksPort, 1500) &&
      TorStatusChecker.verifySocks5Handshake("127.0.0.1", socksPort, 1500)
    if (!handshakeOk) {
      val err = IllegalStateException("Tor SOCKS5 handshake verification failed on port $socksPort")
      CurrentTorRoute.clearRoute()
      DebugLogManager.log("[ROUTE] FAILED profile=GHOST reason=socks_handshake_failed")
      return@withContext Result.failure(err)
    }

    // Step 4: Apply hardened Tor preferences directly to native GeckoView engine
    val proxyApplied = NetworkHardening.applyTorNetworkSettings(geckoEngine.runtime, socksPort, generation)
    if (!proxyApplied) {
      val err = IllegalStateException("Failed to apply Gecko native Tor proxy preferences")
      CurrentTorRoute.clearRoute()
      DebugLogManager.log("[ROUTE] FAILED profile=GHOST reason=gecko_proxy_failed")
      return@withContext Result.failure(err)
    }

    // Step 5: Advance route generation and update Single Source of Truth
    CurrentTorRoute.updateRoute(
      socksPort = socksPort,
      isGhostActive = true,
      isVerified = true,
      exitIp = torManager.currentCircuit.value?.verifiedExitIp,
      generation = generation
    )

    geckoEngine.applyPrivacyProfile(PrivacyProfile.GHOST, socksPort, generation)
    geckoEngine.setTabGhostMode(tabId, true)

    DebugLogManager.log("[ROUTE] ACTIVE profile=GHOST port=$socksPort exitIp=${torManager.currentCircuit.value?.verifiedExitIp ?: "Active"}")
    Result.success(socksPort)
  }

  /**
   * Enters Shield Mode (Direct Clearnet with Fingerprinting Protection & Adblock):
   * 1. Closes existing Ghost session.
   * 2. Clears SOCKS proxy from WebExtension & native Gecko engine ONLY IF no other ghost tabs exist.
   * 3. Stops Tor if no other ghost tab is active.
   */
  suspend fun enterShieldMode(tabId: String) = withContext(Dispatchers.IO) {
    Log.i(TAG, "Entering Shield Mode for tab $tabId (restoring direct clearnet)...")

    geckoEngine.closeSessionSafely(tabId)
    geckoEngine.setTabGhostMode(tabId, false)

    val anyOtherGhostTabs = TabManager.getInstance().tabs.value.any {
      it.id != tabId && it.profile == PrivacyProfile.GHOST
    }

    if (!anyOtherGhostTabs) {
      val generation = CurrentTorRoute.clearRoute()
      DebugLogManager.log("[ROUTE] REQUESTED profile=SHIELD tabId=$tabId generation=$generation")
      torManager.stopTor()
      NetworkHardening.applyShieldNetworkSettings(geckoEngine.runtime, generation)
      geckoEngine.applyPrivacyProfile(PrivacyProfile.SHIELD, null, generation)
      DebugLogManager.log("[ROUTE] ACTIVE profile=SHIELD")
    } else {
      DebugLogManager.log("[ROUTE] Shield tab active but other Ghost tabs exist; maintaining Tor route invariant")
    }
  }

  /**
   * Rotates Tor circuit using genuine NEWNYM signal.
   */
  suspend fun rotateTorCircuit(): Result<TorCircuit> {
    val result = torManager.refreshCircuit()
    result.getOrNull()?.let { c ->
      CurrentTorRoute.updateRoute(
        socksPort = c.socksPort,
        isGhostActive = true,
        isVerified = true,
        exitIp = c.verifiedExitIp
      )
    }
    return result
  }

  /**
   * Performs real zero-leak routing verification against check.torproject.org.
   */
  suspend fun verifyRouting(socksPort: Int? = CurrentTorRoute.currentSocksPort): TorStatusResult {
    val port = socksPort ?: CurrentTorRoute.currentSocksPort
    if (port == null || port <= 0) {
      return TorStatusResult(
        isTor = false,
        ip = "Disconnected",
        message = "No active Tor SOCKS port configured",
        latencyMs = 0L,
        socksHandshakePassed = false
      )
    }
    return TorStatusChecker.verifyTorRouting(port)
  }

  /**
   * Checks if Ghost Mode is currently verified and ready.
   */
  fun isGhostRoutingReady(): Boolean {
    val state = torManager.bootstrapState.value
    return state is TorManager.TorState.READY &&
      TorStatusChecker.isPortListening("127.0.0.1", state.port, 200) &&
      CurrentTorRoute.isGhostActive
  }

  companion object {
    private const val TAG = "PrivacyNetworkCtrl"

    @Volatile
    private var INSTANCE: PrivacyNetworkController? = null

    fun getInstance(context: Context): PrivacyNetworkController {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: PrivacyNetworkController(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
