cat << 'INNER_EOF' > app/src/main/java/com/remmi/browser/engine/TabManager.kt
package com.remmi.browser.engine

import android.content.Context
import android.util.Log
import com.remmi.browser.RemmiApp
import com.remmi.browser.reader.ReaderArticle
import com.remmi.browser.security.PrivacyProfile
import com.remmi.browser.util.DebugLogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TabManager {

  private fun getDefaultProfile(): PrivacyProfile {
    val prefs = RemmiApp.appContext.getSharedPreferences("netrunner_sec_prefs", Context.MODE_PRIVATE)
    val profileStr = prefs.getString("default_profile", PrivacyProfile.SHIELD.name)
    return try {
      PrivacyProfile.valueOf(profileStr ?: PrivacyProfile.SHIELD.name)
    } catch (e: Exception) {
      PrivacyProfile.SHIELD
    }
  }

  private val _tabs = MutableStateFlow<List<BrowserTab>>(
    listOf(
      BrowserTab(
        id = UUID.randomUUID().toString(),
        url = "about:blank",
        title = "New Tab",
        profile = getDefaultProfile(),
      )
    )
  )

  val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

  private val _activeTabIndex = MutableStateFlow(0)
  val activeTabIndex: StateFlow<Int> = _activeTabIndex.asStateFlow()

  val activeTab: BrowserTab?
    get() {
      val tabs = _tabs.value
      val index = _activeTabIndex.value
      return if (index in tabs.indices) tabs[index] else null
    }

  fun openTab(url: String = "about:blank", profile: PrivacyProfile = getDefaultProfile(), isDesktop: Boolean = false) {
    val newTab = BrowserTab(
      id = UUID.randomUUID().toString(),
      url = url,
      title = "New Tab",
      profile = profile,
      isDesktopMode = isDesktop,
    )
    _tabs.value = _tabs.value + newTab
    _activeTabIndex.value = _tabs.value.lastIndex
  }

  fun openTabInBackground(url: String, profile: PrivacyProfile = getDefaultProfile(), isDesktop: Boolean = false) {
    val newTab = BrowserTab(
      id = UUID.randomUUID().toString(),
      url = url,
      title = "New Tab",
      profile = profile,
      isDesktopMode = isDesktop,
    )
    _tabs.value = _tabs.value + newTab
  }

  fun openOrNavigateTab(url: String, profile: PrivacyProfile = getDefaultProfile()) {
    val currentTab = activeTab
    if (currentTab != null && (currentTab.url == "about:blank" || currentTab.url.isEmpty())) {
      updateTab(currentTab.id) { it.copy(url = url, title = "Loading...", profile = profile) }
    } else {
      openTab(url = url, profile = profile)
    }
  }

  fun updateTab(tabId: String, update: (BrowserTab) -> BrowserTab) {
    _tabs.value = _tabs.value.map { if (it.id == tabId) update(it) else it }
  }

  fun switchToTab(tabId: String) {
    val index = _tabs.value.indexOfFirst { it.id == tabId }
    if (index >= 0) {
      _activeTabIndex.value = index
    }
  }

  fun closeTab(tabId: String) {
    val currentTabs = _tabs.value
    val tabToCloseIndex = currentTabs.indexOfFirst { it.id == tabId }
    if (tabToCloseIndex < 0) return

    val newTabs = currentTabs.filter { it.id != tabId }
    if (newTabs.isEmpty()) {
      _tabs.value = listOf(
        BrowserTab(
          id = UUID.randomUUID().toString(),
          url = "about:blank",
          title = "New Tab",
          profile = getDefaultProfile(),
        )
      )
      _activeTabIndex.value = 0
    } else {
      _tabs.value = newTabs
      if (_activeTabIndex.value >= newTabs.size) {
        _activeTabIndex.value = newTabs.lastIndex
      } else if (tabToCloseIndex < _activeTabIndex.value) {
        _activeTabIndex.value -= 1
      }
    }
  }

  fun togglePrivacyProfile() {
    val currentTab = activeTab ?: return
    val newProfile = if (currentTab.profile == PrivacyProfile.SHIELD) {
      PrivacyProfile.GHOST
    } else {
      PrivacyProfile.SHIELD
    }
    updateTab(currentTab.id) { it.copy(profile = newProfile) }
  }

  fun restoreTabs(savedTabs: List<com.remmi.browser.storage.TabEntity>) {
    if (savedTabs.isEmpty()) return
    val restored = savedTabs.map { entity ->
      val p = try {
        PrivacyProfile.valueOf(entity.profile)
      } catch (e: Exception) {
        PrivacyProfile.SHIELD
      }
      BrowserTab(
        id = entity.id,
        url = entity.url,
        title = entity.title,
        profile = p,
        isDesktopMode = entity.isDesktopMode,
      )
    }
    _tabs.value = restored
    _activeTabIndex.value = 0
  }

  fun clearPrivateTabs() {
    val nonPrivateTabs = _tabs.value.filter { it.profile != PrivacyProfile.GHOST && it.profile != PrivacyProfile.INCOGNITO }
    if (nonPrivateTabs.isEmpty()) {
      resetToSingleBlankTab(getDefaultProfile())
    } else {
      _tabs.value = nonPrivateTabs
      _activeTabIndex.value = _activeTabIndex.value.coerceIn(0, nonPrivateTabs.lastIndex)
    }
    Log.i(TAG, "[PRIVATE_TABS_PURGED] Remaining active tabs: ${_tabs.value.size}")
  }

  fun resetToSingleBlankTab(defaultProfile: PrivacyProfile = getDefaultProfile()) {
    val blankTab = BrowserTab(
      id = UUID.randomUUID().toString(),
      url = "about:blank",
      title = "New Tab",
      profile = defaultProfile,
    )
    _tabs.value = listOf(blankTab)
    _activeTabIndex.value = 0
    Log.i(TAG, "[TAB_RESET_BLANK] id=${blankTab.id} profile=$defaultProfile")
    DebugLogManager.log("[TAB_RESET_BLANK] id=${blankTab.id} profile=$defaultProfile")
  }

  fun closeAllTabs(defaultProfile: PrivacyProfile = getDefaultProfile()) {
    resetToSingleBlankTab(defaultProfile)
  }

  companion object {
    private const val TAG = "TabManager"
    @Volatile
    private var INSTANCE: TabManager? = null

    fun getInstance(): TabManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: TabManager().also { INSTANCE = it }
      }
    }
  }
}
INNER_EOF
