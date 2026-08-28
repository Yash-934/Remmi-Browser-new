package com.remmi.browser.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.remmi.browser.storage.BookmarkItem
import com.remmi.browser.storage.HistoryItem
import com.remmi.browser.ui.theme.CyberMonoFamily
import com.remmi.browser.ui.theme.ThemeCyber
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    initialTab: Int = 0,
    onSelectUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    
    val historyList by viewModel.historyList.collectAsState()
    val bookmarksList by viewModel.bookmarksList.collectAsState()
    
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeCyber.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(ThemeCyber.colors.textMuted.copy(alpha = 0.35f))
            )
        }

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVITY",
                color = ThemeCyber.colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ThemeCyber.colors.surfaceLight,
                border = BorderStroke(0.8.dp, ThemeCyber.colors.surfaceBorder),
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onDismiss)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ThemeCyber.colors.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ThemeCyber.colors.surface,
            border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
            modifier = Modifier.fillMaxWidth().height(42.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ThemeCyber.colors.primary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text("Search activity...", color = ThemeCyber.colors.textMuted, fontSize = 12.5.sp)
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        singleLine = true,
                        textStyle = TextStyle(color = ThemeCyber.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(ThemeCyber.colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = ThemeCyber.colors.textMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = ThemeCyber.colors.primary,
            divider = { HorizontalDivider(color = ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f)) },
            indicator = { tabPositions ->
                SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ThemeCyber.colors.primary,
                    height = 2.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("History", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                unselectedContentColor = ThemeCyber.colors.textSecondary
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Bookmarks", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                unselectedContentColor = ThemeCyber.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        if (selectedTab == 0) {
            HistoryTabContent(
                historyList = historyList,
                dateFilter = dateFilter,
                onFilterChanged = { viewModel.updateDateFilter(it) },
                onSelectUrl = { onSelectUrl(it); onDismiss() },
                onDelete = { viewModel.deleteHistory(it) },
                onBookmark = { viewModel.addBookmark(it.title, it.url) },
                onClearAll = { showClearConfirmDialog = true }
            )
        } else {
            BookmarksTabContent(
                bookmarksList = bookmarksList,
                selectedFolder = selectedFolder,
                onFolderChanged = { viewModel.updateFolder(it) },
                onSelectUrl = { onSelectUrl(it); onDismiss() },
                onDelete = { viewModel.deleteBookmark(it) }
            )
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = ThemeCyber.colors.surface,
            title = { Text("Clear History", color = ThemeCyber.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all browsing history? This action cannot be undone.", color = ThemeCyber.colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); showClearConfirmDialog = false }) {
                    Text("Clear All", color = ThemeCyber.colors.dangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = ThemeCyber.colors.textPrimary)
                }
            }
        )
    }
}

@Composable
fun HistoryTabContent(
    historyList: List<HistoryItem>,
    dateFilter: DateFilter,
    onFilterChanged: (DateFilter) -> Unit,
    onSelectUrl: (String) -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onBookmark: (HistoryItem) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    
    // Filters Row
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DateFilter.values()) { filter ->
            val name = when (filter) {
                DateFilter.ALL -> "All Time"
                DateFilter.TODAY -> "Today"
                DateFilter.YESTERDAY -> "Yesterday"
                DateFilter.LAST_7_DAYS -> "Last 7 Days"
            }
            FilterChip(
                selected = dateFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(name, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = ThemeCyber.colors.surface,
                    selectedContainerColor = ThemeCyber.colors.primary.copy(alpha = 0.2f),
                    labelColor = ThemeCyber.colors.textSecondary,
                    selectedLabelColor = ThemeCyber.colors.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ThemeCyber.colors.surfaceBorder,
                    selectedBorderColor = ThemeCyber.colors.primary,
                    enabled = true,
                    selected = dateFilter == filter
                )
            )
        }
    }

    val groupedHistory = remember(historyList) {
        historyList
            .groupBy { getDateGroupKey(it.timestamp) }
            .mapKeys { (_, items) -> formatTimestampToRelativeDateString(items.first().timestamp) }
    }

    if (groupedHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, tint = ThemeCyber.colors.textMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No history found", color = ThemeCyber.colors.textMuted, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            groupedHistory.forEach { (dateHeader, items) ->
                item {
                    Text(
                        text = dateHeader,
                        color = ThemeCyber.colors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(items, key = { it.id }) { item ->
                    HistoryCardItem(
                        item = item,
                        onItemClick = { onSelectUrl(item.url) },
                        onDelete = { onDelete(item) },
                        onBookmark = { onBookmark(item) },
                        onShare = { shareUrl(context, item.title, item.url) }
                    )
                }
            }
            item {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text("CLEAR ALL HISTORY", color = ThemeCyber.colors.dangerRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BookmarksTabContent(
    bookmarksList: List<BookmarkItem>,
    selectedFolder: String,
    onFolderChanged: (String) -> Unit,
    onSelectUrl: (String) -> Unit,
    onDelete: (BookmarkItem) -> Unit
) {
    val folders = listOf("All", "General", "Work", "Personal", "Social")
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Folders Column (Left)
        LazyColumn(
            modifier = Modifier.weight(0.3f).fillMaxHeight().padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(folders) { folder ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedFolder == folder) ThemeCyber.colors.primary.copy(alpha = 0.15f) else Color.Transparent,
                    modifier = Modifier.fillMaxWidth().clickable { onFolderChanged(folder) }
                ) {
                    Text(
                        text = folder,
                        color = if (selectedFolder == folder) ThemeCyber.colors.primary else ThemeCyber.colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selectedFolder == folder) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                    )
                }
            }
        }

        // Bookmarks Column (Right)
        if (bookmarksList.isEmpty()) {
            Box(modifier = Modifier.weight(0.7f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text("No bookmarks in this folder", color = ThemeCyber.colors.textMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(0.7f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(bookmarksList, key = { it.id }) { item ->
                    BookmarkCardItem(
                        item = item,
                        onItemClick = { onSelectUrl(item.url) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCardItem(
    item: HistoryItem,
    onItemClick: () -> Unit,
    onDelete: () -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val domain = remember(item.url) { 
        try { android.net.Uri.parse(item.url).host?.removePrefix("www.") ?: item.url } catch (_: Exception) { item.url }
    }
    val faviconUrl = remember(item.url) { getFaviconUrl(item.url) }
    val initialLetter = remember(domain) { domain.firstOrNull()?.uppercaseChar()?.toString() ?: "W" }
    var isImageError by remember(item.url) { mutableStateOf(false) }

    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormatter.format(Date(item.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ThemeCyber.colors.surface,
        border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ThemeCyber.colors.surfaceLight,
                border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isImageError) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(faviconUrl).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onError = { isImageError = true },
                            modifier = Modifier.size(24.dp).clip(CircleShape)
                        )
                    }
                    if (isImageError) {
                        Text(text = initialLetter, color = ThemeCyber.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { item.url },
                    color = ThemeCyber.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = domain,
                        color = ThemeCyber.colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = ThemeCyber.colors.textMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeStr,
                        color = ThemeCyber.colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.StarBorder, contentDescription = "Star", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onBookmark() })
                Icon(Icons.Default.Share, contentDescription = "Share", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onShare() })
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onDelete() })
            }
        }
    }
}

@Composable
fun BookmarkCardItem(
    item: BookmarkItem,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val domain = remember(item.url) { 
        try { android.net.Uri.parse(item.url).host?.removePrefix("www.") ?: item.url } catch (_: Exception) { item.url }
    }
    val faviconUrl = remember(item.url) { getFaviconUrl(item.url) }
    val initialLetter = remember(domain) { domain.firstOrNull()?.uppercaseChar()?.toString() ?: "W" }
    var isImageError by remember(item.url) { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ThemeCyber.colors.surface,
        border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ThemeCyber.colors.surfaceLight,
                border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isImageError) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(faviconUrl).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            onError = { isImageError = true },
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                    if (isImageError) {
                        Text(text = initialLetter, color = ThemeCyber.colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { item.url },
                    color = ThemeCyber.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = domain,
                    color = ThemeCyber.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// Helpers
private fun formatTimestampToRelativeDateString(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = cal.get(Calendar.YEAR)
    
    cal.timeInMillis = timestamp
    val targetDay = cal.get(Calendar.DAY_OF_YEAR)
    val targetYear = cal.get(Calendar.YEAR)
    
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val formattedDate = sdf.format(Date(timestamp))
    
    return when {
        todayYear == targetYear && today == targetDay -> "TODAY"
        todayYear == targetYear && today - targetDay == 1 -> "YESTERDAY"
        else -> formattedDate.uppercase(Locale.getDefault())
    }
}

private fun getDateGroupKey(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun shareUrl(context: Context, title: String, url: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
