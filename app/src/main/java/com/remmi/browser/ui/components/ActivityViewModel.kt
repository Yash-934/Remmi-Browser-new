package com.remmi.browser.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.remmi.browser.storage.BookmarkDao
import com.remmi.browser.storage.HistoryDao
import com.remmi.browser.storage.HistoryItem
import com.remmi.browser.storage.BookmarkItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilter {
    ALL, TODAY, YESTERDAY, LAST_7_DAYS
}

class ActivityViewModel(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter = _dateFilter.asStateFlow()

    private val _selectedFolder = MutableStateFlow("General")
    val selectedFolder = _selectedFolder.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateDateFilter(filter: DateFilter) {
        _dateFilter.value = filter
    }

    fun updateFolder(folder: String) {
        _selectedFolder.value = folder
    }

    @OptIn(FlowPreview::class)
    val historyList: StateFlow<List<HistoryItem>> = combine(
        _searchQuery.debounce(300),
        _dateFilter
    ) { query, filter ->
        Pair(query, filter)
    }.flatMapLatest { (query, filter) ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val (minTime, maxTime) = when (filter) {
            DateFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                Pair(calendar.timeInMillis, now)
            }
            DateFilter.YESTERDAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val endOfYesterday = calendar.timeInMillis - 1
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                Pair(calendar.timeInMillis, endOfYesterday)
            }
            DateFilter.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                Pair(calendar.timeInMillis, now)
            }
            DateFilter.ALL -> Pair(0L, Long.MAX_VALUE)
        }
        historyDao.getFilteredHistory(query, minTime, maxTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val bookmarksList: StateFlow<List<BookmarkItem>> = combine(
        _searchQuery.debounce(300),
        _selectedFolder
    ) { query, folder ->
        Pair(query, folder)
    }.flatMapLatest { (query, folder) ->
        if (query.isNotBlank()) {
            flow { emit(bookmarkDao.searchBookmarks(query)) }
        } else {
            if (folder == "All") bookmarkDao.getAllBookmarks() else bookmarkDao.getBookmarksByCategory(folder)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearHistory() }
    }

    fun deleteHistory(item: HistoryItem) {
        viewModelScope.launch { historyDao.delete(item) }
    }

    fun deleteBookmark(item: BookmarkItem) {
        viewModelScope.launch { bookmarkDao.delete(item) }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            bookmarkDao.insert(BookmarkItem(url = url, title = title, category = _selectedFolder.value))
        }
    }
}

class ActivityViewModelFactory(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivityViewModel(historyDao, bookmarkDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
