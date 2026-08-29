package com.remmi.browser.engine

import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoPreferenceController as NativePrefCtrl
import org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference
import kotlin.coroutines.resume

class GeckoPreferenceController(private val runtime: GeckoRuntime?) {
  companion object {
    const val PREF_BRANCH_USER: Int = NativePrefCtrl.PREF_BRANCH_USER
    const val PREF_BRANCH_DEFAULT: Int = NativePrefCtrl.PREF_BRANCH_DEFAULT
    private const val TAG = "GeckoPreferenceCtrl"
  }

  suspend fun getPreferences(keys: List<String>): Map<String, Any?> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
    try {
      NativePrefCtrl.getGeckoPrefs(keys.toMutableList()).accept(
        { result ->
          if (cont.isActive) cont.resume(result as? Map<String, Any?> ?: emptyMap())
        },
        {
          if (cont.isActive) cont.resume(emptyMap())
        }
      )
    } catch (t: Throwable) {
      if (cont.isActive) cont.resume(emptyMap())
    }
  }

  suspend fun applyPreferences(prefs: Map<String, Any>, branch: Int = PREF_BRANCH_USER): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
    if (prefs.isEmpty()) {
      cont.resume(true)
      return@suspendCancellableCoroutine
    }
    
    val setList = mutableListOf<SetGeckoPreference<*>>()
    for ((name, value) in prefs) {
      when (value) {
        is String -> setList.add(SetGeckoPreference.setStringPref(name, value, branch))
        is Int -> setList.add(SetGeckoPreference.setIntPref(name, value, branch))
        is Boolean -> setList.add(SetGeckoPreference.setBoolPref(name, value, branch))
        else -> {
          Log.e(TAG, "Unsupported preference type for key=$name: ${value::class.java.simpleName}")
          if (cont.isActive) cont.resume(false)
          return@suspendCancellableCoroutine
        }
      }
    }
    
    try {
      NativePrefCtrl.setGeckoPrefs(setList).accept(
        { result ->
          val failedPrefs = result?.filterValues { it != true }?.keys ?: emptySet()
          if (failedPrefs.isNotEmpty()) {
            val criticalFails = failedPrefs.filter { isRequiredForGhost(it) }
            Log.e(TAG, "[ROUTE] GECKO_PREF_FAILURE error=Some preferences rejected: $failedPrefs")
            if (criticalFails.isNotEmpty()) {
              Log.e(TAG, "[ROUTE] GECKO_PREF_FAILURE error=CRITICAL Ghost preferences failed: $criticalFails")
              if (cont.isActive) cont.resume(false)
            } else {
              Log.w(TAG, "Non-critical preferences failed: $failedPrefs")
              if (cont.isActive) cont.resume(true)
            }
          } else {
            Log.d(TAG, "Successfully applied ${prefs.size} native Gecko preferences (branch=$branch)")
            if (cont.isActive) cont.resume(true)
          }
        },
        { error ->
          Log.e(TAG, "[ROUTE] GECKO_PREF_FAILURE error=${error?.message}", error)
          if (cont.isActive) cont.resume(false)
        }
      )
    } catch (t: Throwable) {
      Log.e(TAG, "[ROUTE] GECKO_PREF_FAILURE exception=${t.javaClass.simpleName} message=${t.message}", t)
      if (cont.isActive) cont.resume(false)
    }
  }

  private fun isRequiredForGhost(prefName: String): Boolean {
    return prefName.startsWith("network.proxy.") ||
           prefName.startsWith("network.dns.") ||
           prefName == "privacy.resistFingerprinting" ||
           prefName == "privacy.firstparty.isolate" ||
           prefName == "media.peerconnection.enabled"
  }
}
