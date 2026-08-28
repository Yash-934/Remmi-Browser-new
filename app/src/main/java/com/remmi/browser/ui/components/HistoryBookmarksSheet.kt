package com.remmi.browser.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

// Helper functions for formatting dates
private fun formatTimestampToRelativeDateString(timestamp: Long): String {
  val itemCal = Calendar.getInstance().apply { timeInMillis = timestamp }
  val nowCal = Calendar.getInstance()

  val isSameYear = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
  val isToday = isSameYear && itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

  val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
  val isYesterday = itemCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
      itemCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

  val datePattern = "MMM d, yyyy"
  val formattedDate = SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(timestamp))

  return when {
    isToday -> "Today - $formattedDate"
    isYesterday -> "Yesterday - $formattedDate"
    else -> formattedDate
  }
}

private fun getDateGroupKey(timestamp: Long): String {
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  return sdf.format(Date(timestamp))
}

@Composable
fun HistoryBookmarksSheet(
  initialTab: Int = 0, // 0 = History, 1 = Bookmarks
  historyList: List<HistoryItem>,
  bookmarksList: List<BookmarkItem>,
  onSelectUrl: (String) -> Unit,
  onDeleteHistory: (HistoryItem) -> Unit,
  onClearAllHistory: () -> Unit,
  onDeleteBookmark: (BookmarkItem) -> Unit,
  onAddBookmark: () -> Unit = {},
  onSaveToReadingList: () -> Unit = {},
  onCreateCollection: () -> Unit = {},
  onSyncStatus: () -> Unit = {},
  onShareUrl: (String) -> Unit = {},
  onAddHistoryItemToBookmark: (HistoryItem) -> Unit = {},
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedTab by remember { mutableIntStateOf(initialTab) }
  var searchQuery by remember { mutableStateOf("") }
  val accentColor = ThemeCyber.colors.primary

  val filteredHistory = remember(historyList, searchQuery) {
    if (searchQuery.isBlank()) historyList
    else historyList.filter {
      it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
    }
  }

  val filteredBookmarks = remember(bookmarksList, searchQuery) {
    if (searchQuery.isBlank()) bookmarksList
    else bookmarksList.filter {
      it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
    }
  }

  val groupedHistory = remember(filteredHistory) {
    filteredHistory
      .sortedByDescending { it.timestamp }
      .groupBy { getDateGroupKey(it.timestamp) }
      .mapKeys { (_, items) -> formatTimestampToRelativeDateString(items.first().timestamp) }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ThemeCyber.colors.background)
      .navigationBarsPadding()
      .statusBarsPadding()
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    // Top Bar Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
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

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (historyList.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = ThemeCyber.colors.dangerRed,
            modifier = Modifier.clickable { onClearAllHistory() }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Clear All",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "CLEAR ALL HISTORY",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
        }

        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Close",
          tint = ThemeCyber.colors.textPrimary,
          modifier = Modifier
            .size(24.dp)
            .clickable { onDismiss() }
        )
      }
    }

    // Main Tab Selector
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = Color.Transparent,
      contentColor = accentColor,
      divider = { HorizontalDivider(color = ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f)) },
      indicator = { tabPositions ->
        SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = accentColor,
          height = 3.dp,
        )
      },
      modifier = Modifier.fillMaxWidth()
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedTab == 0) accentColor else ThemeCyber.colors.textSecondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("HISTORY (${filteredHistory.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium, color = if (selectedTab == 0) ThemeCyber.colors.textPrimary else ThemeCyber.colors.textSecondary)
          }
        }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (selectedTab == 1) accentColor else ThemeCyber.colors.textSecondary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("BOOKMARKS (${filteredBookmarks.size})", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium, color = if (selectedTab == 1) ThemeCyber.colors.textPrimary else ThemeCyber.colors.textSecondary)
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
      val chips = listOf("Today", "Yesterday", "Last 7 Days", "Specific Date", "Most Visited")
      items(chips) { chip ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (chip == "Today") ThemeCyber.colors.primary.copy(alpha = 0.15f) else ThemeCyber.colors.surface,
          border = BorderStroke(1.dp, if (chip == "Today") ThemeCyber.colors.primary.copy(alpha = 0.5f) else ThemeCyber.colors.surfaceBorder),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = chip,
              color = if (chip == "Today") ThemeCyber.colors.primary else ThemeCyber.colors.textPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
            if (chip == "Specific Date") {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(12.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(ThemeCyber.colors.surface)
        .border(1.dp, ThemeCyber.colors.surfaceBorder, RoundedCornerShape(22.dp))
        .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = ThemeCyber.colors.textMuted,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      BasicTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        textStyle = TextStyle(
          color = ThemeCyber.colors.textPrimary,
          fontSize = 14.sp,
        ),
        cursorBrush = SolidColor(accentColor),
        singleLine = true,
        modifier = Modifier.weight(1f),
        decorationBox = { inner ->
          if (searchQuery.isEmpty()) {
            Text(
              text = "Search...",
              color = ThemeCyber.colors.textMuted,
              fontSize = 14.sp,
            )
          }
          inner()
        }
      )
      Icon(
        imageVector = Icons.Default.Tune,
        contentDescription = "Filter",
        tint = ThemeCyber.colors.textMuted,
        modifier = Modifier.size(18.dp),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4 Action Cards Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      ActionCard(icon = Icons.Default.StarBorder, title = "ADD\nBOOKMARK", modifier = Modifier.weight(1f), onClick = onAddBookmark)
      ActionCard(icon = Icons.Default.MenuBook, title = "SAVE TO\nREADING LIST", modifier = Modifier.weight(1f), onClick = onSaveToReadingList)
      ActionCard(icon = Icons.Default.CreateNewFolder, title = "CREATE\nCOLLECTION", modifier = Modifier.weight(1f), onClick = onCreateCollection)
      ActionCard(icon = Icons.Default.Sync, title = "SYNC\nSTATUS", modifier = Modifier.weight(1f), onClick = onSyncStatus)
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Main Content List
    if (selectedTab == 0) {
      // History List
      if (groupedHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
          Text(
            text = if (searchQuery.isNotEmpty()) "No matching history entries" else "No browsing history yet",
            color = ThemeCyber.colors.textMuted,
            fontSize = 14.sp,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          groupedHistory.forEach { (dateHeader, itemsInDay) ->
            item(key = "header_$dateHeader") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = dateHeader,
                  color = ThemeCyber.colors.textPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                )
                Icon(
                  imageVector = Icons.Default.CalendarToday,
                  contentDescription = null,
                  tint = ThemeCyber.colors.textSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            items(itemsInDay, key = { "hist_${it.id}" }) { item ->
              HistoryItemCard(
                item = item,
                onItemClick = {
                  onSelectUrl(item.url)
                  onDismiss()
                },
                onDelete = { onDeleteHistory(item) },
                onBookmark = { onAddHistoryItemToBookmark(item) },
                onShare = { onShareUrl(item.url) }
              )
            }
          }
        }
      }
    } else {
      // Bookmarks List - Redesigned Bookmarks Pane Layout
      if (filteredBookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
          Text(
            text = if (searchQuery.isNotEmpty()) "No matching bookmarks" else "No bookmarks saved yet",
            color = ThemeCyber.colors.textMuted,
            fontSize = 14.sp,
          )
        }
      } else {
        Column(modifier = Modifier.weight(1f)) {
          // Inner Bookmark Tabs
          TabRow(
            selectedTabIndex = 0,
            containerColor = ThemeCyber.colors.surfaceLight,
            contentColor = accentColor,
            divider = { HorizontalDivider(color = ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f)) },
            indicator = { tabPositions ->
              SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[0]),
                color = accentColor,
                height = 3.dp,
              )
            },
            modifier = Modifier.clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
          ) {
            Tab(selected = true, onClick = {}, text = { Text("ALL BOOKMARKS (${filteredBookmarks.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ThemeCyber.colors.textPrimary) })
            Tab(selected = false, onClick = {}, text = { Text("READING LIST (0)", fontSize = 11.sp, color = ThemeCyber.colors.textSecondary) })
            Tab(selected = false, onClick = {}, text = { Text("COLLECTIONS (0)", fontSize = 11.sp, color = ThemeCyber.colors.textSecondary) })
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .background(ThemeCyber.colors.surfaceLight)
              .padding(8.dp)
          ) {
            // Folders Pane
            Column(
              modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .padding(end = 8.dp)
            ) {
              FolderItem("Work", expanded = true, selected = true)
              FolderItem("Personal", expanded = false, selected = false)
              FolderItem("Social", expanded = false, selected = false)
            }

            // Cards Pane
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(filteredBookmarks, key = { "bm_${it.id}" }) { item ->
                BookmarkDetailedCard(
                  item = item,
                  onItemClick = {
                    onSelectUrl(item.url)
                    onDismiss()
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, modifier: Modifier, onClick: () -> Unit = {}) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = ThemeCyber.colors.surface,
    border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
    modifier = modifier.aspectRatio(1f).clickable { onClick() }
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = ThemeCyber.colors.primary, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = title,
        color = ThemeCyber.colors.textPrimary,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        lineHeight = 11.sp
      )
    }
  }
}

@Composable
private fun FolderItem(name: String, expanded: Boolean, selected: Boolean) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(if (selected) ThemeCyber.colors.primary.copy(alpha = 0.2f) else Color.Transparent)
      .padding(horizontal = 6.dp, vertical = 8.dp)
  ) {
    Icon(
      imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
      contentDescription = null,
      tint = ThemeCyber.colors.textSecondary,
      modifier = Modifier.size(14.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Icon(
      imageVector = Icons.Default.Folder,
      contentDescription = null,
      tint = ThemeCyber.colors.textSecondary,
      modifier = Modifier.size(14.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = name,
      color = ThemeCyber.colors.textPrimary,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
private fun HistoryItemCard(
  item: HistoryItem,
  onItemClick: () -> Unit,
  onDelete: () -> Unit,
  onBookmark: () -> Unit = {},
  onShare: () -> Unit = {},
) {
  val context = LocalContext.current
  val domain = remember(item.url) { 
    try {
      android.net.Uri.parse(item.url).host?.removePrefix("www.") ?: item.url
    } catch (_: Exception) { item.url }
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
    shadowElevation = if (ThemeCyber.colors.isLight) 2.dp else 0.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onItemClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.Top
    ) {
      // Big Circular Logo
      Box(
        modifier = Modifier
          .size(50.dp)
          .clip(CircleShape)
          .background(ThemeCyber.colors.surfaceLight)
          .border(0.6.dp, ThemeCyber.colors.surfaceBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        if (!isImageError) {
          AsyncImage(
            model = ImageRequest.Builder(context).data(faviconUrl).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onError = { isImageError = true },
            modifier = Modifier.size(30.dp).clip(CircleShape)
          )
        }
        if (isImageError) {
          Text(text = initialLetter, color = ThemeCyber.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Middle Details
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = domain,
            color = ThemeCyber.colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.Default.Link, contentDescription = null, tint = ThemeCyber.colors.primary, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Timestamp: $timeStr",
          color = ThemeCyber.colors.textPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Action Icons Column
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Default.StarBorder, contentDescription = "Star", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onBookmark() })
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onDelete() })
        Icon(Icons.Default.Share, contentDescription = "Share", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onShare() })
      }
    }
  }
}

@Composable
private fun BookmarkDetailedCard(
  item: BookmarkItem,
  onItemClick: () -> Unit
) {
  val context = LocalContext.current
  val domain = remember(item.url) { 
    try {
      android.net.Uri.parse(item.url).host?.removePrefix("www.") ?: item.url
    } catch (_: Exception) { item.url }
  }
  val faviconUrl = remember(item.url) { getFaviconUrl(item.url) }
  val initialLetter = remember(domain) { domain.firstOrNull()?.uppercaseChar()?.toString() ?: "W" }
  var isImageError by remember(item.url) { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = ThemeCyber.colors.surface,
    border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
    shadowElevation = if (ThemeCyber.colors.isLight) 2.dp else 0.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onItemClick)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Row(verticalAlignment = Alignment.Top) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(ThemeCyber.colors.surfaceLight)
            .border(0.6.dp, ThemeCyber.colors.surfaceBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          if (!isImageError) {
            AsyncImage(
              model = ImageRequest.Builder(context).data(faviconUrl).crossfade(true).build(),
              contentDescription = null,
              contentScale = ContentScale.Fit,
              onError = { isImageError = true },
              modifier = Modifier.size(28.dp).clip(CircleShape)
            )
          }
          if (isImageError) {
            Text(text = initialLetter, color = ThemeCyber.colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title.ifBlank { item.url },
            color = ThemeCyber.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = domain,
            color = ThemeCyber.colors.textSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Tagged: #BrowserDesign #DevTools",
            color = ThemeCyber.colors.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
      
      Spacer(modifier = Modifier.height(12.dp))
      
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { },
          modifier = Modifier.weight(1f).height(32.dp),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(0.dp),
          border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder)
        ) {
          Text("Edit", color = ThemeCyber.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Button(
          onClick = { },
          modifier = Modifier.weight(1.5f).height(32.dp),
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(0.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.textSecondary)
        ) {
          Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add to Reading List", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
