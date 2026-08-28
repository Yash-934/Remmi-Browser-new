package com.remmi.browser.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Extracts clean domain name for display (e.g. "www.booking.com" or "booking.com").
 */
fun extractDisplayDomain(url: String): String {
  return try {
    val uri = Uri.parse(url)
    uri.host ?: url
  } catch (_: Exception) {
    url
  }
}

/**
 * Returns the favicon URL for a given domain/URL.
 */
fun getWebsiteFaviconUrl(url: String): String {
  if (com.remmi.browser.security.NetworkRouteAuthority.isOnionDestination(url)) {
    return "" // Zero external clearnet leaks for Tor hidden services
  }
  return try {
    val uri = Uri.parse(url)
    val host = uri.host?.removePrefix("www.") ?: url.split("/").firstOrNull() ?: url
    "https://www.google.com/s2/favicons?domain=$host&sz=128"
  } catch (_: Exception) {
    "https://www.google.com/s2/favicons?domain=google.com&sz=128"
  }
}

/**
 * Formats a timestamp into a human-readable date header like:
 * - "Today - Aug 28, 2026"
 * - "Yesterday - Aug 27, 2026"
 * - "Aug 26, 2026"
 */
private fun formatDateGroupHeader(timestamp: Long): String {
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

  // Group history items by calendar date in chronological order (newest first)
  val groupedHistory = remember(filteredHistory) {
    val map = linkedMapOf<String, Pair<String, MutableList<HistoryItem>>>()
    for (item in filteredHistory) {
      val groupKey = getDateGroupKey(item.timestamp)
      val headerTitle = formatDateGroupHeader(item.timestamp)
      val entry = map.getOrPut(groupKey) { Pair(headerTitle, mutableListOf()) }
      entry.second.add(item)
    }
    map.values.toList()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ThemeCyber.colors.background)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Top Bar Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = if (selectedTab == 0) "History" else "Bookmarks",
        color = ThemeCyber.colors.textPrimary,
        fontFamily = CyberMonoFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (selectedTab == 0 && historyList.isNotEmpty()) {
          TextButton(
            onClick = onClearAllHistory,
            modifier = Modifier.testTag("clear_history_button")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Clear All",
              tint = ThemeCyber.colors.dangerRed,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Clear All",
              color = ThemeCyber.colors.dangerRed,
              fontFamily = CyberMonoFamily,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(ThemeCyber.colors.surfaceLight)
            .testTag("close_history_sheet_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = ThemeCyber.colors.textPrimary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Modern Search Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(42.dp)
        .clip(RoundedCornerShape(21.dp))
        .background(ThemeCyber.colors.surface)
        .border(0.8.dp, ThemeCyber.colors.surfaceBorder, RoundedCornerShape(21.dp))
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = null,
        tint = ThemeCyber.colors.textMuted,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      BasicTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        textStyle = TextStyle(
          color = ThemeCyber.colors.textPrimary,
          fontFamily = CyberMonoFamily,
          fontSize = 13.sp,
        ),
        cursorBrush = SolidColor(accentColor),
        singleLine = true,
        modifier = Modifier
          .weight(1f)
          .testTag("search_history_bookmarks_input"),
        decorationBox = { inner ->
          if (searchQuery.isEmpty()) {
            Text(
              text = if (selectedTab == 0) "Search history..." else "Search bookmarks...",
              color = ThemeCyber.colors.textMuted,
              fontFamily = CyberMonoFamily,
              fontSize = 13.sp,
            )
          }
          inner()
        }
      )
      if (searchQuery.isNotEmpty()) {
        IconButton(
          onClick = { searchQuery = "" },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Clear",
            tint = ThemeCyber.colors.textMuted,
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Tab Selector
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = ThemeCyber.colors.surface,
      contentColor = accentColor,
      indicator = { tabPositions ->
        SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = accentColor,
          height = 2.5.dp,
        )
      },
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("HISTORY (${filteredHistory.size})", fontFamily = CyberMonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        },
        modifier = Modifier.testTag("history_tab_button")
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("BOOKMARKS (${filteredBookmarks.size})", fontFamily = CyberMonoFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        },
        modifier = Modifier.testTag("bookmarks_tab_button")
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Content List
    if (selectedTab == 0) {
      // Date-Wise Grouped History with Logos
      if (groupedHistory.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
          Text(
            text = if (searchQuery.isNotEmpty()) "No matching history entries" else "No browsing history yet",
            color = ThemeCyber.colors.textMuted,
            fontFamily = CyberMonoFamily,
            fontSize = 13.sp,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(vertical = 6.dp)
        ) {
          groupedHistory.forEach { (dateHeader, itemsInDay) ->
            // Date Section Header (e.g. "Yesterday - Aug 27, 2026", "Aug 26, 2026")
            item(key = "header_$dateHeader") {
              Text(
                text = dateHeader,
                color = ThemeCyber.colors.textSecondary,
                fontFamily = CyberMonoFamily,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
              )
            }

            // History Items for this day
            items(itemsInDay, key = { "hist_${it.id}" }) { item ->
              HistoryOrBookmarkItemRow(
                title = item.title.ifBlank { item.url },
                url = item.url,
                onItemClick = {
                  onSelectUrl(item.url)
                  onDismiss()
                },
                onDelete = { onDeleteHistory(item) }
              )
            }
          }
        }
      }
    } else {
      // Bookmarks List with Logos
      if (filteredBookmarks.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
          Text(
            text = if (searchQuery.isNotEmpty()) "No matching bookmarks" else "No bookmarks saved yet",
            color = ThemeCyber.colors.textMuted,
            fontFamily = CyberMonoFamily,
            fontSize = 13.sp,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          items(filteredBookmarks, key = { "bm_${it.id}" }) { item ->
            HistoryOrBookmarkItemRow(
              title = item.title.ifBlank { item.url },
              url = item.url,
              onItemClick = {
                onSelectUrl(item.url)
                onDismiss()
              },
              onDelete = { onDeleteBookmark(item) }
            )
          }
        }
      }
    }
  }
}

/**
 * Individual row matching the screenshot style:
 * - Left: Circular Website Logo / Favicon avatar
 * - Middle: Title (maxLines = 1, Ellipsis) + Subtitle Domain (e.g. www.booking.com)
 * - Right: Circular Close / Delete (X) button
 */
@Composable
private fun HistoryOrBookmarkItemRow(
  title: String,
  url: String,
  onItemClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val context = LocalContext.current
  val domain = remember(url) { extractDisplayDomain(url) }
  val faviconUrl = remember(url) { getWebsiteFaviconUrl(url) }
  val initialLetter = remember(domain) {
    domain.removePrefix("www.").firstOrNull()?.uppercaseChar()?.toString() ?: "W"
  }

  var isImageError by remember(url) { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onItemClick)
      .padding(horizontal = 4.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // 1. Circular Website Logo / Avatar
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(ThemeCyber.colors.surfaceLight.copy(alpha = 0.8f))
        .border(0.6.dp, ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      if (!isImageError) {
        AsyncImage(
          model = ImageRequest.Builder(context)
            .data(faviconUrl)
            .crossfade(true)
            .build(),
          contentDescription = "$domain logo",
          contentScale = ContentScale.Fit,
          onError = { isImageError = true },
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
        )
      }

      if (isImageError) {
        // Monogram initial fallback
        Text(
          text = initialLetter,
          color = ThemeCyber.colors.primary,
          fontFamily = CyberMonoFamily,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // 2. Title & Domain Subtitle
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = title,
        color = ThemeCyber.colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = domain,
        color = ThemeCyber.colors.textMuted,
        fontFamily = CyberMonoFamily,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    // 3. Close / Delete (X) Button matching screenshot
    IconButton(
      onClick = onDelete,
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .testTag("delete_item_button")
    ) {
      Icon(
        imageVector = Icons.Default.Cancel,
        contentDescription = "Delete item",
        tint = ThemeCyber.colors.textMuted.copy(alpha = 0.8f),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
