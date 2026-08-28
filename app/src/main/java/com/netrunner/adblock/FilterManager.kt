package com.netrunner.adblock

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class FilterSubscription(
  val id: String,
  val title: String,
  val description: String,
  val ruleCount: Int,
  val enabled: Boolean,
  val url: String,
  val isCustom: Boolean = false,
)

class FilterManager(
  private val adblockBridge: AdblockBridge,
  private val context: Context? = null,
) {

  private val prefs: SharedPreferences? = context?.getSharedPreferences("remmi_filter_subs", Context.MODE_PRIVATE)

  private val defaultList = listOf(
    FilterSubscription(
      id = "easylist",
      title = "EasyList Core",
      description = "Primary ad-blocking filter list for standard tracking and display ads.",
      ruleCount = 48500,
      enabled = prefs?.getBoolean("filter_easylist", true) ?: true,
      url = "https://easylist.to/easylist/easylist.txt"
    ),
    FilterSubscription(
      id = "easyprivacy",
      title = "EasyPrivacy (Anti-Telemetry)",
      description = "Blocks behavioral trackers, web analytics, and telemetry beacons.",
      ruleCount = 31200,
      enabled = prefs?.getBoolean("filter_easyprivacy", true) ?: true,
      url = "https://easylist.to/easylist/easyprivacy.txt"
    ),
    FilterSubscription(
      id = "fanboy_annoyance",
      title = "Fanboy's Annoyance List",
      description = "Removes popups, cookie consent overlays, and social media tracking widgets.",
      ruleCount = 22400,
      enabled = prefs?.getBoolean("filter_fanboy_annoyance", true) ?: true,
      url = "https://easylist.to/easylist/fanboy-annoyance.txt"
    ),
    FilterSubscription(
      id = "brave_unbreak",
      title = "Remmi Anti-Malware / Phishing",
      description = "Blocks known cryptominers, malicious redirects, and hostile domains.",
      ruleCount = 14800,
      enabled = prefs?.getBoolean("filter_brave_unbreak", true) ?: true,
      url = "https://raw.githubusercontent.com/netrunner/filters/main/unbreak.txt"
    ),
  )

  private val _subscriptions = MutableStateFlow<List<FilterSubscription>>(defaultList)
  val subscriptions: StateFlow<List<FilterSubscription>> = _subscriptions.asStateFlow()

  fun toggleSubscription(id: String) {
    _subscriptions.value = _subscriptions.value.map { sub ->
      if (sub.id == id) {
        val newState = !sub.enabled
        prefs?.edit()?.putBoolean("filter_${sub.id}", newState)?.apply()
        sub.copy(enabled = newState)
      } else sub
    }
  }

  fun getTotalActiveRules(): Int {
    return _subscriptions.value.filter { it.enabled }.sumOf { it.ruleCount }
  }

  companion object {
    @Volatile
    private var INSTANCE: FilterManager? = null

    fun getInstance(context: Context, adblockBridge: AdblockBridge = AdblockBridge()): FilterManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: FilterManager(adblockBridge, context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
