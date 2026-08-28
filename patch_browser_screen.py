import re

with open('app/src/main/java/com/remmi/browser/ui/screens/BrowserScreen.kt', 'r') as f:
    content = f.read()

# Replace HistoryBookmarksSheet import with ActivityScreen & ViewModel
import_history = "import com.remmi.browser.ui.components.HistoryBookmarksSheet"
import_activity = "import com.remmi.browser.ui.components.ActivityScreen\nimport com.remmi.browser.ui.components.ActivityViewModel\nimport com.remmi.browser.ui.components.ActivityViewModelFactory\nimport androidx.lifecycle.viewmodel.compose.viewModel"

content = content.replace(import_history, import_activity)

# Instantiate ActivityViewModel
viewModel_injection = """  val activeTab = tabs.getOrNull(activeTabIndex) ?: tabs.firstOrNull() ?: BrowserTab()"""
new_viewModel_injection = """  val activeTab = tabs.getOrNull(activeTabIndex) ?: tabs.firstOrNull() ?: BrowserTab()
  val activityViewModel: ActivityViewModel = viewModel(
    factory = ActivityViewModelFactory(database.historyDao(), database.bookmarkDao())
  )"""

content = content.replace(viewModel_injection, new_viewModel_injection)

# Replace HistoryBookmarksSheet usage with ActivityScreen
history_sheet_pattern = r"""      HistoryBookmarksSheet\([\s\S]*?onDismiss = \{ showHistoryBookmarksSheet = false \},\n      \)"""
activity_screen_usage = """      ActivityScreen(
        viewModel = activityViewModel,
        initialTab = historyBookmarksInitialTab,
        onSelectUrl = { url ->
          tabManager.updateTab(activeTab.id) { it.copy(url = url, isReaderMode = false, readerArticle = null) }
        },
        onDismiss = { showHistoryBookmarksSheet = false }
      )"""

content = re.sub(history_sheet_pattern, activity_screen_usage, content)

with open('app/src/main/java/com/remmi/browser/ui/screens/BrowserScreen.kt', 'w') as f:
    f.write(content)

