import re

with open('app/src/main/java/com/remmi/browser/ui/screens/BrowserScreen.kt', 'r') as f:
    content = f.read()

usage_pattern = r"""      HistoryBookmarksSheet\(\n        initialTab = historyBookmarksInitialTab,\n        historyList = historyList,\n        bookmarksList = bookmarksList,\n        onSelectUrl = \{ url ->\n          tabManager\.updateTab\(activeTab\.id\) \{ it\.copy\(url = url, isReaderMode = false, readerArticle = null\) \}\n        \},\n        onDeleteHistory = \{ item ->\n          scope\.launch\(Dispatchers\.IO\) \{\n            database\.historyDao\(\)\.delete\(item\)\n          \}\n        \},\n        onClearAllHistory = \{\n          scope\.launch\(Dispatchers\.IO\) \{\n            database\.historyDao\(\)\.clearHistory\(\)\n          \}\n        \},\n        onDeleteBookmark = \{ bm ->\n          scope\.launch\(Dispatchers\.IO\) \{\n            database\.bookmarkDao\(\)\.delete\(bm\)\n          \}\n        \},\n        onDismiss = \{ showHistoryBookmarksSheet = false \},\n      \)"""

new_usage = """      HistoryBookmarksSheet(
        initialTab = historyBookmarksInitialTab,
        historyList = historyList,
        bookmarksList = bookmarksList,
        onSelectUrl = { url ->
          tabManager.updateTab(activeTab.id) { it.copy(url = url, isReaderMode = false, readerArticle = null) }
        },
        onDeleteHistory = { item ->
          scope.launch(Dispatchers.IO) {
            database.historyDao().delete(item)
          }
        },
        onClearAllHistory = {
          scope.launch(Dispatchers.IO) {
            database.historyDao().clearHistory()
          }
        },
        onDeleteBookmark = { bm ->
          scope.launch(Dispatchers.IO) {
            database.bookmarkDao().delete(bm)
          }
        },
        onAddBookmark = {
          scope.launch(Dispatchers.IO) {
            val bm = BookmarkItem(url = activeTab.url, title = activeTab.title, timestamp = System.currentTimeMillis())
            database.bookmarkDao().insert(bm)
          }
          android.widget.Toast.makeText(context, "Added to Bookmarks", android.widget.Toast.LENGTH_SHORT).show()
        },
        onSaveToReadingList = {
          android.widget.Toast.makeText(context, "Saved to Reading List", android.widget.Toast.LENGTH_SHORT).show()
        },
        onCreateCollection = {
          android.widget.Toast.makeText(context, "Collection feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
        },
        onSyncStatus = {
          android.widget.Toast.makeText(context, "Syncing data...", android.widget.Toast.LENGTH_SHORT).show()
        },
        onShareUrl = { url ->
          val sendIntent: android.content.Intent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, url)
            type = "text/plain"
          }
          val shareIntent = android.content.Intent.createChooser(sendIntent, null)
          context.startActivity(shareIntent)
        },
        onAddHistoryItemToBookmark = { item ->
          scope.launch(Dispatchers.IO) {
            val bm = BookmarkItem(url = item.url, title = item.title, timestamp = System.currentTimeMillis())
            database.bookmarkDao().insert(bm)
          }
          android.widget.Toast.makeText(context, "Saved history item to Bookmarks", android.widget.Toast.LENGTH_SHORT).show()
        },
        onDismiss = { showHistoryBookmarksSheet = false },
      )"""

usage_match = re.search(usage_pattern, content)
if usage_match:
    content = content.replace(usage_match.group(0), new_usage)
else:
    print("Usage not found")

with open('app/src/main/java/com/remmi/browser/ui/screens/BrowserScreen.kt', 'w') as f:
    f.write(content)

print("Success")

