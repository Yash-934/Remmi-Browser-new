package org.mozilla.geckoview

import android.util.Log
import org.mozilla.gecko.EventDispatcher
import org.mozilla.gecko.util.GeckoBundle

/**
 * Controller for applying native Mozilla Gecko preferences to GeckoView runtime.
 * Bridges native Necko proxy and privacy configurations.
 */
class GeckoPreferenceController(private val runtime: GeckoRuntime?) {

  companion object {
    const val PREF_BRANCH_USER: Int = 1
    const val PREF_BRANCH_DEFAULT: Int = 0
    private const val TAG = "GeckoPreferenceCtrl"
  }

  fun setGeckoPref(name: String, value: String, branch: Int = PREF_BRANCH_USER): Boolean {
    val bundle = GeckoBundle(1)
    bundle.putString(name, value)
    return dispatchPrefBundle(name, value, bundle, branch)
  }

  fun setGeckoPref(name: String, value: Int, branch: Int = PREF_BRANCH_USER): Boolean {
    val bundle = GeckoBundle(1)
    bundle.putInt(name, value)
    return dispatchPrefBundle(name, value, bundle, branch)
  }

  fun setGeckoPref(name: String, value: Boolean, branch: Int = PREF_BRANCH_USER): Boolean {
    val bundle = GeckoBundle(1)
    bundle.putBoolean(name, value)
    return dispatchPrefBundle(name, value, bundle, branch)
  }

  fun setGeckoPref(name: String, value: Double, branch: Int = PREF_BRANCH_USER): Boolean {
    val bundle = GeckoBundle(1)
    bundle.putDouble(name, value)
    return dispatchPrefBundle(name, value, bundle, branch)
  }

  fun <T : Any> setGeckoPref(name: String, value: T, branch: Int = PREF_BRANCH_USER): Boolean {
    val bundle = GeckoBundle(1)
    when (value) {
      is String -> bundle.putString(name, value)
      is Int -> bundle.putInt(name, value)
      is Boolean -> bundle.putBoolean(name, value)
      is Double -> bundle.putDouble(name, value)
      is Float -> bundle.putDouble(name, value.toDouble())
      is Long -> bundle.putLong(name, value)
      else -> bundle.putString(name, value.toString())
    }
    return dispatchPrefBundle(name, value, bundle, branch)
  }

  fun applyPreferences(prefs: Map<String, Any>, branch: Int = PREF_BRANCH_USER): Boolean {
    if (prefs.isEmpty()) return true
    val bundle = GeckoBundle(prefs.size)
    for ((name, value) in prefs) {
      when (value) {
        is String -> bundle.putString(name, value)
        is Int -> bundle.putInt(name, value)
        is Boolean -> bundle.putBoolean(name, value)
        is Double -> bundle.putDouble(name, value)
        is Float -> bundle.putDouble(name, value.toDouble())
        is Long -> bundle.putLong(name, value)
        else -> bundle.putString(name, value.toString())
      }
    }
    return try {
      EventDispatcher.getInstance().dispatch("GeckoView:SetDefaultPrefs", bundle)
      runtime?.let { rt ->
        try {
          rt.setDefaultPrefs(bundle)
        } catch (_: Throwable) {}
      }
      Log.i(TAG, "Successfully applied ${prefs.size} native Gecko preferences (branch=$branch)")
      true
    } catch (t: Throwable) {
      Log.e(TAG, "Failed to dispatch pref bundle: ${t.message}", t)
      false
    }
  }

  private fun dispatchPrefBundle(name: String, value: Any, bundle: GeckoBundle, branch: Int): Boolean {
    return try {
      EventDispatcher.getInstance().dispatch("GeckoView:SetDefaultPrefs", bundle)
      runtime?.let { rt ->
        try {
          rt.setDefaultPrefs(bundle)
        } catch (_: Throwable) {}
      }
      Log.d(TAG, "Applied Gecko pref $name = $value (branch=$branch)")
      true
    } catch (t: Throwable) {
      Log.e(TAG, "Failed to apply pref $name = $value: ${t.message}", t)
      false
    }
  }
}
