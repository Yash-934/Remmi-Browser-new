import re

with open('app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt', 'r') as f:
    content = f.read()

history_dao_patch = """  @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 200")
  fun getAllHistory(): Flow<List<HistoryItem>>

  @Query("SELECT * FROM history WHERE (:query = '' OR title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%') AND timestamp >= :minTimestamp AND timestamp <= :maxTimestamp ORDER BY timestamp DESC")
  fun getFilteredHistory(query: String, minTimestamp: Long, maxTimestamp: Long): Flow<List<HistoryItem>>"""

content = content.replace("""  @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 200")
  fun getAllHistory(): Flow<List<HistoryItem>>""", history_dao_patch)


bookmark_dao_patch = """  @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
  fun getAllBookmarks(): Flow<List<BookmarkItem>>

  @Query("SELECT * FROM bookmarks WHERE category = :folder ORDER BY timestamp DESC")
  fun getBookmarksByCategory(folder: String): Flow<List<BookmarkItem>>"""

content = content.replace("""  @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
  fun getAllBookmarks(): Flow<List<BookmarkItem>>""", bookmark_dao_patch)

with open('app/src/main/java/com/remmi/browser/storage/NetRunnerDatabase.kt', 'w') as f:
    f.write(content)
