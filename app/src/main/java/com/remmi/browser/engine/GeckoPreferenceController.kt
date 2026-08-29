package com.remmi.browser.engine

import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoPreferenceController as NativePrefCtrl
import org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference

class GeckoPreferenceController(private val runtime: GeckoRuntime?) {
  companion object {
    const val PREF_BRANCH_USER: Int = NativePrefCtrl.PREF_BRANCH_USER
    const val PREF_BRANCH_DEFAULT: Int = NativePrefCtrl.PREF_BRANCH_DEFAULT
    private const val TAG = "GeckoPreferenceCtrl"
  }

  fun applyPreferences(prefs: Map<String, Any>, branch: Int = PREF_BRANCH_USER): Boolean {
    if (prefs.isEmpty()) return true
    
    val setList = mutableListOf<SetGeckoPreference<*>>()
    for ((name, value) in prefs) {
      when (value) {
        is String -> setList.add(SetGeckoPreference.setStringPref(name, value, branch))
        is Int -> setList.add(SetGeckoPreference.setIntPref(name, value, branch))
        is Boolean -> setList.add(SetGeckoPreference.setBoolPref(name, value, branch))
        // double/float/long mappings are not strictly supported by the Gecko pref API (usually only Int/Boolean/String).
        // Let's fallback to int for numbers, string for others.
        is Double -> setList.add(SetGeckoPreference.setIntPref(name, value.toInt(), branch))
        is Float -> setList.add(SetGeckoPreference.setIntPref(name, value.toInt(), branch))
        is Long -> setList.add(SetGeckoPreference.setIntPref(name, value.toInt(), branch))
        else -> setList.add(SetGeckoPreference.setStringPref(name, value.toString(), branch))
      }
    }
    
    return try {
      val result = NativePrefCtrl.setGeckoPrefs(setList).poll(10000L)
      if (result == null) {
          Log.e(TAG, "Failed to apply Gecko preferences: timed out or null result")
          return false
      }
      val anyFailed = result.values.any { it != true }
      if (anyFailed) {
          Log.e(TAG, "Failed to apply some Gecko preferences")
          return false
      }
      Log.i(TAG, "Successfully applied ${prefs.size} native Gecko preferences (branch=$branch)")
      true
    } catch (t: Throwable) {
      Log.e(TAG, "Failed to apply Gecko preferences: ${t.message}", t)
      false
    }
  }
}
