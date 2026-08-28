package com.remmi.browser.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.remmi.browser.R
import com.remmi.browser.engine.BrowserTab
import com.remmi.browser.engine.TabGroup
import com.remmi.browser.security.PrivacyProfile
import com.remmi.browser.security.SecurityLevel
import com.remmi.browser.ui.theme.CyberMonoFamily
import com.remmi.browser.ui.theme.ThemeCyber

@Composable
fun TabStrip(
  tabs: List<BrowserTab>,
  tabGroups: List<TabGroup> = emptyList(),
  activeIndex: Int,
  onTabSelect: (Int) -> Unit,
  onTabClose: (String) -> Unit,
  onNewTab: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(ThemeCyber.colors.backgroundDarker)
      .padding(vertical = 4.dp, horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    LazyRow(
      modifier = Modifier.weight(1f),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
      itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
        val isActive = index == activeIndex
        val group = tabGroups.find { it.id == tab.groupId }
        val groupColor = group?.let { Color(it.colorHex) }
        val activeColor = if (tab.profile == PrivacyProfile.GHOST) {
          ThemeCyber.colors.torPurple
        } else {
          groupColor ?: ThemeCyber.colors.primary
        }

        Row(
          modifier = Modifier
            .width(148.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) ThemeCyber.colors.surfaceLight else ThemeCyber.colors.surface)
            .border(
              width = if (isActive) 1.2.dp else 0.6.dp,
              color = if (isActive) activeColor else (groupColor?.copy(alpha = 0.5f) ?: ThemeCyber.colors.surfaceBorder),
              shape = RoundedCornerShape(6.dp)
            )
            .clickable { onTabSelect(index) }
            .padding(horizontal = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          // Group color dot or profile icon
          if (groupColor != null) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(groupColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
          }

          if (tab.profile == PrivacyProfile.INCOGNITO) {
            Icon(
              painter = painterResource(R.drawable.ic_incognito),
              contentDescription = "Incognito Tab",
              tint = if (isActive) ThemeCyber.colors.textPrimary else ThemeCyber.colors.textMuted,
              modifier = Modifier.size(13.dp),
            )
          } else {
            Icon(
              imageVector = if (tab.profile == PrivacyProfile.GHOST) Icons.Default.VpnKey else Icons.Default.Shield,
              contentDescription = null,
              tint = if (isActive) activeColor else ThemeCyber.colors.textMuted,
              modifier = Modifier.size(12.dp),
            )
          }

          Spacer(modifier = Modifier.width(5.dp))

          Text(
            text = tab.title.ifEmpty { "New Tab" },
            color = if (isActive) ThemeCyber.colors.textPrimary else ThemeCyber.colors.textSecondary,
            fontFamily = CyberMonoFamily,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )

          IconButton(
            onClick = { onTabClose(tab.id) },
            modifier = Modifier.size(18.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Tab",
              tint = if (isActive) activeColor else ThemeCyber.colors.textMuted,
              modifier = Modifier.size(11.dp),
            )
          }
        }
      }
    }

    IconButton(
      onClick = onNewTab,
      modifier = Modifier
        .size(32.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(ThemeCyber.colors.surfaceLight)
        .testTag("add_new_tab_button"),
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "New Tab",
        tint = ThemeCyber.colors.primary,
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

enum class TabFilter {
  ALL,
  RECENT,
  ACTIVE,
  GROUPS,
  GHOST,
  INACTIVE
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabGridSheet(
  tabs: List<BrowserTab>,
  tabGroups: List<TabGroup>,
  activeIndex: Int,
  onTabSelect: (Int) -> Unit,
  onTabClose: (String) -> Unit,
  onNewTab: (PrivacyProfile, groupId: String?) -> Unit,
  onCreateGroup: (title: String, colorHex: Long, tabIds: List<String>) -> Unit,
  onAddTabToGroup: (tabId: String, groupId: String) -> Unit,
  onRemoveTabFromGroup: (tabId: String) -> Unit,
  onUpdateGroup: (groupId: String, title: String, colorHex: Long) -> Unit,
  onDeleteGroup: (groupId: String, closeTabs: Boolean) -> Unit,
  onToggleGroupCollapse: (groupId: String) -> Unit,
  onSetTabInactive: (tabId: String, isInactive: Boolean) -> Unit,
  onSetGroupInactive: (groupId: String, isInactive: Boolean) -> Unit,
  onCloseAllInactiveTabs: () -> Unit,
  onDuplicateTab: (tabId: String) -> Unit,
  onTogglePinTab: (tabId: String) -> Unit,
  onCloseAllTabs: () -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedFilter by remember { mutableStateOf(TabFilter.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedSpaceFilter by remember { mutableStateOf<String?>(null) } // null = all, "personal", "incognito", "tor", or groupId

  // Dialog States
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var editingGroup by remember { mutableStateOf<TabGroup?>(null) }
  var tabForGroupAssignment by remember { mutableStateOf<BrowserTab?>(null) }
  var tabOptionsTarget by remember { mutableStateOf<BrowserTab?>(null) }

  // Counts
  val personalTabs = remember(tabs) { tabs.filter { it.profile == PrivacyProfile.SHIELD && it.groupId == null } }
  val incognitoTabs = remember(tabs) { tabs.filter { it.profile == PrivacyProfile.INCOGNITO } }
  val torTabs = remember(tabs) { tabs.filter { it.profile == PrivacyProfile.GHOST } }
  val inactiveTabs = remember(tabs) { tabs.filter { it.isInactive } }
  val activeOnlyTabs = remember(tabs) { tabs.filter { !it.isInactive } }

  // Filtered & Sorted Tabs
  val filteredTabs = remember(tabs, selectedFilter, selectedSpaceFilter, searchQuery) {
    var list = when (selectedSpaceFilter) {
      "personal" -> tabs.filter { it.profile == PrivacyProfile.SHIELD && it.groupId == null }
      "incognito" -> incognitoTabs
      "tor" -> torTabs
      null -> tabs
      else -> tabs.filter { it.groupId == selectedSpaceFilter }
    }

    list = when (selectedFilter) {
      TabFilter.ALL -> list
      TabFilter.RECENT -> list.sortedByDescending { it.lastAccessedAt }
      TabFilter.ACTIVE -> list.filter { !it.isInactive }
      TabFilter.GROUPS -> list.filter { it.groupId != null }
      TabFilter.GHOST -> list.filter { it.profile == PrivacyProfile.GHOST }
      TabFilter.INACTIVE -> list.filter { it.isInactive }
    }

    val query = searchQuery.trim().lowercase()
    if (query.isNotEmpty()) {
      list = list.filter {
        it.title.lowercase().contains(query) || it.url.lowercase().contains(query)
      }
    }
    list
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(ThemeCyber.colors.background)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 14.dp, vertical = 6.dp)
  ) {
    // 1. DRAG HANDLE & BRAND HEADER BAR
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
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

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Brand Logo + Title
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = ThemeCyber.colors.primary.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, ThemeCyber.colors.primary.copy(alpha = 0.4f)),
          modifier = Modifier.size(30.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "R",
              color = ThemeCyber.colors.primary,
              fontFamily = CyberMonoFamily,
              fontWeight = FontWeight.Black,
              fontSize = 15.sp
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Remmi",
          color = ThemeCyber.colors.textPrimary,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = CyberMonoFamily
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
          shape = CircleShape,
          color = ThemeCyber.colors.surfaceLight,
          border = BorderStroke(0.6.dp, ThemeCyber.colors.surfaceBorder)
        ) {
          Text(
            text = "${tabs.size}",
            color = ThemeCyber.colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = CyberMonoFamily,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
          )
        }
      }

      // Top Actions: + New Tab & Close Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Quick + New Tab
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ThemeCyber.colors.primary.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, ThemeCyber.colors.primary.copy(alpha = 0.5f)),
          modifier = Modifier
            .size(34.dp)
            .clickable {
              onNewTab(PrivacyProfile.SHIELD, null)
              onDismiss()
            }
            .testTag("add_tab_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Default.Add,
              contentDescription = "New Tab",
              tint = ThemeCyber.colors.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        // Close Sheet (X)
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ThemeCyber.colors.surfaceLight,
          border = BorderStroke(0.8.dp, ThemeCyber.colors.surfaceBorder),
          modifier = Modifier
            .size(34.dp)
            .clickable(onClick = onDismiss)
            .testTag("close_tabs_sheet_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Close Tabs",
              tint = ThemeCyber.colors.textPrimary,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 2. SEARCH TABS AND SPACES INPUT BAR
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = ThemeCyber.colors.surface,
      border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .height(42.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search Tabs and Spaces",
          tint = ThemeCyber.colors.primary,
          modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (searchQuery.isEmpty()) {
            Text(
              text = "Search Tabs and Spaces",
              color = ThemeCyber.colors.textMuted,
              fontSize = 12.5.sp,
            )
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(
              color = ThemeCyber.colors.textPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            ),
            cursorBrush = SolidColor(ThemeCyber.colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { searchQuery = "" },
            modifier = Modifier.size(24.dp)
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Clear search",
              tint = ThemeCyber.colors.textMuted,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 3. MAIN SCROLLABLE CONTENT (SPACES + CHIPS + TABS)
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // SPACES SECTION
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Spaces Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "SPACES",
              color = ThemeCyber.colors.textPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
            if (tabGroups.isNotEmpty() || personalTabs.isNotEmpty() || incognitoTabs.isNotEmpty() || torTabs.isNotEmpty()) {
              Text(
                text = if (selectedSpaceFilter != null) "Show All" else "Manage",
                color = ThemeCyber.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                  .clickable {
                    if (selectedSpaceFilter != null) selectedSpaceFilter = null else showCreateGroupDialog = true
                  }
                  .padding(4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Spaces Horizontal Scroll Cards
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            // 1. Personal Space Card
            item {
              SpaceCard(
                title = "Personal Space",
                count = personalTabs.size,
                isSelected = selectedSpaceFilter == "personal",
                icon = {
                  Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(24.dp)
                  )
                },
                accentColor = Color(0xFF007AFF),
                onClick = {
                  selectedSpaceFilter = if (selectedSpaceFilter == "personal") null else "personal"
                }
              )
            }

            // 2. Incognito Space Card
            item {
              SpaceCard(
                title = "Incognito Space",
                count = incognitoTabs.size,
                isSelected = selectedSpaceFilter == "incognito",
                icon = {
                  Icon(
                    painter = painterResource(R.drawable.ic_incognito),
                    contentDescription = null,
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(22.dp)
                  )
                },
                accentColor = Color(0xFF8E8E93),
                onClick = {
                  selectedSpaceFilter = if (selectedSpaceFilter == "incognito") null else "incognito"
                }
              )
            }

            // 3. Tor Space Card
            item {
              SpaceCard(
                title = "Tor Space",
                count = torTabs.size,
                isSelected = selectedSpaceFilter == "tor",
                icon = {
                  Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = ThemeCyber.colors.torPurple,
                    modifier = Modifier.size(22.dp)
                  )
                },
                accentColor = ThemeCyber.colors.torPurple,
                onClick = {
                  selectedSpaceFilter = if (selectedSpaceFilter == "tor") null else "tor"
                }
              )
            }

            // 4. Custom User Groups / Spaces
            items(tabGroups, key = { it.id }) { group ->
              val groupCount = tabs.count { it.groupId == group.id }
              val groupColor = Color(group.colorHex)
              SpaceCard(
                title = group.title,
                count = groupCount,
                isSelected = selectedSpaceFilter == group.id,
                icon = {
                  Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = groupColor,
                    modifier = Modifier.size(24.dp)
                  )
                },
                accentColor = groupColor,
                onClick = {
                  selectedSpaceFilter = if (selectedSpaceFilter == group.id) null else group.id
                },
                onLongClick = { editingGroup = group }
              )
            }

            // 5. + New Space Card
            item {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = ThemeCyber.colors.surfaceLight,
                border = BorderStroke(1.2.dp, ThemeCyber.colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                  .width(105.dp)
                  .height(84.dp)
                  .clickable { showCreateGroupDialog = true }
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Space",
                    tint = ThemeCyber.colors.primary,
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "+ New Space",
                    color = ThemeCyber.colors.primary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          }
        }
      }

      // FILTER CHIPS ROW (All, Recent, Active, Sleep)
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          item {
            ModernFilterChip(
              title = "All",
              count = tabs.size,
              icon = Icons.Default.GridView,
              isSelected = selectedFilter == TabFilter.ALL,
              onClick = { selectedFilter = TabFilter.ALL }
            )
          }
          item {
            ModernFilterChip(
              title = "Recent",
              icon = Icons.Default.Schedule,
              isSelected = selectedFilter == TabFilter.RECENT,
              onClick = { selectedFilter = TabFilter.RECENT }
            )
          }
          item {
            ModernFilterChip(
              title = "Active",
              count = activeOnlyTabs.size,
              icon = Icons.Default.CheckCircle,
              colorAccent = ThemeCyber.colors.successGreen,
              isSelected = selectedFilter == TabFilter.ACTIVE,
              onClick = { selectedFilter = TabFilter.ACTIVE }
            )
          }
          item {
            ModernFilterChip(
              title = "Sleep",
              count = inactiveTabs.size,
              icon = Icons.Default.Bedtime,
              colorAccent = ThemeCyber.colors.warningYellow,
              isSelected = selectedFilter == TabFilter.INACTIVE,
              onClick = { selectedFilter = TabFilter.INACTIVE }
            )
          }
        }
      }

      // ALL TABS SECTION HEADER
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (selectedSpaceFilter != null) "FILTERED TABS" else "ALL TABS",
              color = ThemeCyber.colors.textPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = ThemeCyber.colors.primary.copy(alpha = 0.15f),
              border = BorderStroke(0.6.dp, ThemeCyber.colors.primary.copy(alpha = 0.3f))
            ) {
              Text(
                text = "${filteredTabs.size}",
                color = ThemeCyber.colors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = CyberMonoFamily,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          if (tabs.isNotEmpty()) {
            Text(
              text = "Close All",
              color = ThemeCyber.colors.dangerRed,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
                .clickable {
                  onCloseAllTabs()
                  onDismiss()
                }
                .padding(4.dp)
            )
          }
        }
      }

      // INACTIVE SLEEP BANNER (when viewing sleep filter)
      if (selectedFilter == TabFilter.INACTIVE && inactiveTabs.isNotEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ThemeCyber.colors.surfaceLight,
            border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "💤 Dormant / Sleeping Tabs",
                  color = ThemeCyber.colors.textPrimary,
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Unloaded to save battery and RAM.",
                  color = ThemeCyber.colors.textMuted,
                  fontSize = 10.5.sp,
                )
              }
              Button(
                onClick = onCloseAllInactiveTabs,
                colors = ButtonDefaults.buttonColors(
                  containerColor = ThemeCyber.colors.dangerRed.copy(alpha = 0.2f),
                  contentColor = ThemeCyber.colors.dangerRed
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "Close All",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // TAB GRID ITEMS (2 COLUMNS)
      if (filteredTabs.isNotEmpty()) {
        val chunkedTabs = filteredTabs.chunked(2)
        items(chunkedTabs) { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            for (tab in row) {
              val originalIndex = tabs.indexOfFirst { it.id == tab.id }
              val isActive = originalIndex == activeIndex
              val group = tabGroups.find { it.id == tab.groupId }
              val groupColor = group?.let { Color(it.colorHex) }

              Box(modifier = Modifier.weight(1f)) {
                ModernTabCard(
                  tab = tab,
                  isActive = isActive,
                  groupColor = groupColor,
                  onSelect = {
                    if (originalIndex >= 0) {
                      onTabSelect(originalIndex)
                      onDismiss()
                    }
                  },
                  onClose = { onTabClose(tab.id) },
                  onOptions = { tabOptionsTarget = tab }
                )
              }
            }
            if (row.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      } else {
        // Empty State
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.TabUnselected,
                contentDescription = null,
                tint = ThemeCyber.colors.textMuted,
                modifier = Modifier.size(44.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (searchQuery.isNotEmpty()) "No matching tabs found" else "No tabs in this space",
                color = ThemeCyber.colors.textMuted,
                fontSize = 13.sp
              )
            }
          }
        }
      }
    }

    // 4. BOTTOM ACTION BAR (Incognito, Tor, Close All)
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider(color = ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick Incognito Button
        OutlinedButton(
          onClick = {
            onNewTab(PrivacyProfile.INCOGNITO, null)
            onDismiss()
          },
          border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = ThemeCyber.colors.surface),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            painter = painterResource(R.drawable.ic_incognito),
            contentDescription = "New Incognito",
            tint = ThemeCyber.colors.textPrimary,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Incognito",
            fontSize = 12.sp,
            color = ThemeCyber.colors.textPrimary,
            fontWeight = FontWeight.Medium
          )
        }

        // Quick Tor Tab
        OutlinedButton(
          onClick = {
            onNewTab(PrivacyProfile.GHOST, null)
            onDismiss()
          },
          border = BorderStroke(1.dp, ThemeCyber.colors.torPurple.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = ThemeCyber.colors.torPurple.copy(alpha = 0.08f)),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.VpnKey,
            contentDescription = "New Tor Tab",
            tint = ThemeCyber.colors.torPurple,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Tor",
            fontSize = 12.sp,
            color = ThemeCyber.colors.torPurple,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Close All Tabs Action
      TextButton(
        onClick = {
          onCloseAllTabs()
          onDismiss()
        },
        colors = ButtonDefaults.textButtonColors(contentColor = ThemeCyber.colors.dangerRed)
      ) {
        Text(
          text = "Close All",
          fontSize = 12.5.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }

  // --- DIALOGS ---

  // 1. Create Group Dialog
  if (showCreateGroupDialog) {
    CreateTabGroupDialog(
      availableTabs = tabs.filter { it.groupId == null },
      onDismiss = { showCreateGroupDialog = false },
      onCreate = { title, colorHex, selectedTabIds ->
        onCreateGroup(title, colorHex, selectedTabIds)
        showCreateGroupDialog = false
      }
    )
  }

  // 2. Edit Group Dialog
  editingGroup?.let { group ->
    EditTabGroupDialog(
      group = group,
      onDismiss = { editingGroup = null },
      onSave = { newTitle, newColor ->
        onUpdateGroup(group.id, newTitle, newColor)
        editingGroup = null
      },
      onDelete = { closeTabs ->
        onDeleteGroup(group.id, closeTabs)
        editingGroup = null
      }
    )
  }

  // 3. Tab Options Bottom Sheet
  tabOptionsTarget?.let { tab ->
    TabActionSheet(
      tab = tab,
      tabGroups = tabGroups,
      onDismiss = { tabOptionsTarget = null },
      onDuplicate = {
        onDuplicateTab(tab.id)
        tabOptionsTarget = null
      },
      onTogglePin = {
        onTogglePinTab(tab.id)
        tabOptionsTarget = null
      },
      onToggleInactive = {
        onSetTabInactive(tab.id, !tab.isInactive)
        tabOptionsTarget = null
      },
      onAssignToGroup = { gid ->
        if (gid != null) {
          onAddTabToGroup(tab.id, gid)
        } else {
          onRemoveTabFromGroup(tab.id)
        }
        tabOptionsTarget = null
      },
      onCreateGroupWithTab = {
        showCreateGroupDialog = true
        tabOptionsTarget = null
      }
    )
  }
}

// -------------------------------------------------------------
// SPACE CARD (Personal, Incognito, Tor, Custom Groups)
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpaceCard(
  title: String,
  count: Int,
  isSelected: Boolean,
  icon: @Composable () -> Unit,
  accentColor: Color,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = if (isSelected) accentColor.copy(alpha = 0.15f) else ThemeCyber.colors.surface,
    border = BorderStroke(
      width = if (isSelected) 1.8.dp else 1.dp,
      color = if (isSelected) accentColor else ThemeCyber.colors.surfaceBorder
    ),
    modifier = Modifier
      .width(105.dp)
      .height(84.dp)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Box(
        modifier = Modifier
          .size(30.dp),
        contentAlignment = Alignment.Center
      ) {
        icon()
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = title,
          color = if (isSelected) accentColor else ThemeCyber.colors.textPrimary,
          fontSize = 11.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center
        )
        Text(
          text = if (count == 1) "1 Tab" else "$count Tabs",
          color = if (isSelected) accentColor.copy(alpha = 0.85f) else ThemeCyber.colors.textMuted,
          fontSize = 9.5.sp,
          fontWeight = FontWeight.Normal
        )
      }
    }
  }
}

// -------------------------------------------------------------
// MODERN FILTER CHIP
// -------------------------------------------------------------

@Composable
private fun ModernFilterChip(
  title: String,
  count: Int? = null,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  colorAccent: Color = ThemeCyber.colors.primary,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (isSelected) colorAccent.copy(alpha = 0.15f) else ThemeCyber.colors.surface,
    border = BorderStroke(
      width = if (isSelected) 1.2.dp else 0.8.dp,
      color = if (isSelected) colorAccent else ThemeCyber.colors.surfaceBorder
    ),
    modifier = Modifier
      .height(32.dp)
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isSelected) colorAccent else ThemeCyber.colors.textSecondary,
          modifier = Modifier.size(14.dp)
        )
      }
      Text(
        text = if (count != null) "$title ($count)" else title,
        color = if (isSelected) colorAccent else ThemeCyber.colors.textSecondary,
        fontSize = 11.5.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      )
    }
  }
}

// -------------------------------------------------------------
// REDESIGNED 2-COLUMN MODERN TAB CARD
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernTabCard(
  tab: BrowserTab,
  isActive: Boolean,
  groupColor: Color?,
  onSelect: () -> Unit,
  onClose: () -> Unit,
  onOptions: () -> Unit,
) {
  val context = LocalContext.current
  val profileColor = when (tab.profile) {
    PrivacyProfile.GHOST -> ThemeCyber.colors.torPurple
    PrivacyProfile.INCOGNITO -> Color(0xFF8E8E93)
    else -> groupColor ?: ThemeCyber.colors.primary
  }

  val isBlankOrNewTab = tab.url.isEmpty() || tab.url == "about:blank" || tab.url == "netrunner://newtab"
  val cleanDomain = remember(tab.url) {
    try {
      val uri = android.net.Uri.parse(tab.url)
      uri.host?.removePrefix("www.") ?: if (isBlankOrNewTab) "New Tab" else tab.url
    } catch (_: Exception) {
      tab.url
    }
  }

  val faviconUrl = remember(tab.url) {
    com.remmi.browser.ui.components.getFaviconUrl(tab.url)
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = ThemeCyber.colors.surface),
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(
      width = if (isActive) 1.8.dp else 1.dp,
      color = if (isActive) profileColor else (groupColor?.copy(alpha = 0.6f) ?: ThemeCyber.colors.surfaceBorder)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .height(180.dp)
      .combinedClickable(
        onClick = onSelect,
        onLongClick = onOptions
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(
          if (isActive) {
            Brush.verticalGradient(
              colors = listOf(
                profileColor.copy(alpha = 0.09f),
                ThemeCyber.colors.surface
              )
            )
          } else {
            SolidColor(ThemeCyber.colors.surface)
          }
        )
    ) {
      // 1. CARD TOP HEADER (Dot Status, Favicon / Title, 3-dots Menu)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(ThemeCyber.colors.surfaceLight.copy(alpha = 0.6f))
          .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Status Dot (Red/Cyan/Green/Purple)
          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(
                if (tab.profile == PrivacyProfile.GHOST) ThemeCyber.colors.torPurple
                else if (isActive) ThemeCyber.colors.dangerRed
                else profileColor
              )
          )
          Spacer(modifier = Modifier.width(6.dp))

          // Tab Title / Domain
          Text(
            text = tab.title.ifEmpty { cleanDomain },
            color = ThemeCyber.colors.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // 3-dots Overflow Menu
        IconButton(
          onClick = onOptions,
          modifier = Modifier.size(22.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = ThemeCyber.colors.textSecondary,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      // 2. WEBSITE PREVIEW SKELETON CANVAS
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 8.dp, vertical = 6.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(ThemeCyber.colors.backgroundDarker.copy(alpha = 0.5f))
          .border(0.6.dp, ThemeCyber.colors.surfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (isBlankOrNewTab) {
          // New Tab Canvas
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = profileColor.copy(alpha = 0.7f),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = "Remmi Home",
              color = profileColor,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        } else if (tab.profile == PrivacyProfile.GHOST) {
          // Tor Canvas
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VpnKey,
              contentDescription = null,
              tint = ThemeCyber.colors.torPurple,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = cleanDomain,
              color = ThemeCyber.colors.textPrimary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Onion Circuit",
              color = ThemeCyber.colors.torPurple,
              fontSize = 8.5.sp,
            )
          }
        } else {
          // Preview Wireframe with Site Title
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = tab.title.ifEmpty { cleanDomain },
              color = ThemeCyber.colors.textPrimary,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            // Stylized Website Visual Lines
            Column(
              verticalArrangement = Arrangement.spacedBy(3.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.9f)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(ThemeCyber.colors.surfaceBorder)
              )
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.65f)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(ThemeCyber.colors.surfaceBorder.copy(alpha = 0.6f))
              )
            }

            Text(
              text = cleanDomain,
              color = ThemeCyber.colors.textMuted,
              fontSize = 8.5.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Sleep badge overlay
        if (tab.isInactive) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
          ) {
            Text(
              text = "💤 SLEEP",
              fontSize = 7.5.sp,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
      }

      // 3. CARD FOOTER BAR (Close Tab Action)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Close Tab",
          color = ThemeCyber.colors.textSecondary,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier
            .clickable(onClick = onClose)
            .padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// CREATE TAB GROUP DIALOG
// -------------------------------------------------------------

@Composable
private fun CreateTabGroupDialog(
  availableTabs: List<BrowserTab>,
  onDismiss: () -> Unit,
  onCreate: (title: String, colorHex: Long, selectedTabIds: List<String>) -> Unit,
) {
  var groupTitle by remember { mutableStateOf("") }
  var selectedColorHex by remember { mutableStateOf(TabGroup.PRESET_COLORS[0]) }
  val selectedTabIds = remember { mutableStateListOf<String>() }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ThemeCyber.colors.surface,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.CreateNewFolder,
          contentDescription = null,
          tint = Color(selectedColorHex),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "New Tab Group",
          color = ThemeCyber.colors.textPrimary,
          fontFamily = CyberMonoFamily,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Group Name Input
        OutlinedTextField(
          value = groupTitle,
          onValueChange = { groupTitle = it },
          label = { Text("Group Name (e.g. Work, Tor, Research)", fontSize = 12.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(selectedColorHex),
            unfocusedBorderColor = ThemeCyber.colors.surfaceBorder,
            focusedTextColor = ThemeCyber.colors.textPrimary,
            unfocusedTextColor = ThemeCyber.colors.textPrimary
          ),
          textStyle = TextStyle(fontFamily = CyberMonoFamily, fontSize = 13.sp),
          modifier = Modifier.fillMaxWidth()
        )

        // Color Palette Selector
        Text(
          text = "Choose Color Tag:",
          color = ThemeCyber.colors.textSecondary,
          fontFamily = CyberMonoFamily,
          fontSize = 11.sp
        )

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          TabGroup.PRESET_COLORS.forEach { colorVal ->
            val isSelected = selectedColorHex == colorVal
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(colorVal))
                .border(
                  width = if (isSelected) 2.5.dp else 0.dp,
                  color = if (isSelected) Color.White else Color.Transparent,
                  shape = CircleShape
                )
                .clickable { selectedColorHex = colorVal },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = Color.Black,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        // Optional: Include open tabs
        if (availableTabs.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Add open tabs into this group:",
            color = ThemeCyber.colors.textSecondary,
            fontFamily = CyberMonoFamily,
            fontSize = 11.sp
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 130.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(availableTabs, key = { it.id }) { tab ->
              val isChecked = selectedTabIds.contains(tab.id)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isChecked) Color(selectedColorHex).copy(alpha = 0.15f) else ThemeCyber.colors.surfaceLight)
                  .clickable {
                    if (isChecked) selectedTabIds.remove(tab.id) else selectedTabIds.add(tab.id)
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { check ->
                    if (check) selectedTabIds.add(tab.id) else selectedTabIds.remove(tab.id)
                  },
                  colors = CheckboxDefaults.colors(checkedColor = Color(selectedColorHex)),
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = tab.title.ifEmpty { tab.url },
                  color = ThemeCyber.colors.textPrimary,
                  fontFamily = CyberMonoFamily,
                  fontSize = 11.5.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(groupTitle.ifBlank { "Group" }, selectedColorHex, selectedTabIds.toList())
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColorHex))
      ) {
        Text("Create Group", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = CyberMonoFamily)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = ThemeCyber.colors.textMuted, fontFamily = CyberMonoFamily)
      }
    }
  )
}

// -------------------------------------------------------------
// EDIT TAB GROUP DIALOG
// -------------------------------------------------------------

@Composable
private fun EditTabGroupDialog(
  group: TabGroup,
  onDismiss: () -> Unit,
  onSave: (title: String, colorHex: Long) -> Unit,
  onDelete: (closeTabs: Boolean) -> Unit,
) {
  var groupTitle by remember { mutableStateOf(group.title) }
  var selectedColorHex by remember { mutableStateOf(group.colorHex) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ThemeCyber.colors.surface,
    title = {
      Text(
        text = "Edit Tab Group",
        color = ThemeCyber.colors.textPrimary,
        fontFamily = CyberMonoFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = groupTitle,
          onValueChange = { groupTitle = it },
          label = { Text("Group Name", fontSize = 12.sp) },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(selectedColorHex),
            unfocusedBorderColor = ThemeCyber.colors.surfaceBorder,
            focusedTextColor = ThemeCyber.colors.textPrimary,
            unfocusedTextColor = ThemeCyber.colors.textPrimary
          ),
          textStyle = TextStyle(fontFamily = CyberMonoFamily, fontSize = 13.sp),
          modifier = Modifier.fillMaxWidth()
        )

        Text(
          text = "Tag Color:",
          color = ThemeCyber.colors.textSecondary,
          fontFamily = CyberMonoFamily,
          fontSize = 11.sp
        )

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          TabGroup.PRESET_COLORS.forEach { colorVal ->
            val isSelected = selectedColorHex == colorVal
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(colorVal))
                .border(
                  width = if (isSelected) 2.5.dp else 0.dp,
                  color = if (isSelected) Color.White else Color.Transparent,
                  shape = CircleShape
                )
                .clickable { selectedColorHex = colorVal },
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = Color.Black,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Ungroup vs Close Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          OutlinedButton(
            onClick = { onDelete(false) },
            border = BorderStroke(0.8.dp, ThemeCyber.colors.surfaceBorder),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("Ungroup Tabs", fontSize = 10.5.sp, fontFamily = CyberMonoFamily, color = ThemeCyber.colors.textPrimary)
          }

          Button(
            onClick = { onDelete(true) },
            colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.dangerRed.copy(alpha = 0.2f), contentColor = ThemeCyber.colors.dangerRed),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("Close Group & Tabs", fontSize = 10.5.sp, fontFamily = CyberMonoFamily)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(groupTitle.ifBlank { "Group" }, selectedColorHex) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColorHex))
      ) {
        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = CyberMonoFamily)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = ThemeCyber.colors.textMuted, fontFamily = CyberMonoFamily)
      }
    }
  )
}

// -------------------------------------------------------------
// TAB ACTION BOTTOM SHEET
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabActionSheet(
  tab: BrowserTab,
  tabGroups: List<TabGroup>,
  onDismiss: () -> Unit,
  onDuplicate: () -> Unit,
  onTogglePin: () -> Unit,
  onToggleInactive: () -> Unit,
  onAssignToGroup: (groupId: String?) -> Unit,
  onCreateGroupWithTab: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = ThemeCyber.colors.surface,
    dragHandle = { BottomSheetDefaults.DragHandle(color = ThemeCyber.colors.surfaceBorder) }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .navigationBarsPadding(),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Header info
      Text(
        text = tab.title.ifEmpty { tab.url },
        color = ThemeCyber.colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CyberMonoFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = tab.url,
        color = ThemeCyber.colors.textMuted,
        fontSize = 11.sp,
        fontFamily = CyberMonoFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(6.dp))

      // 1. Pin / Unpin Tab
      TabActionRow(
        icon = Icons.Default.PushPin,
        title = if (tab.isPinned) "Unpin Tab" else "Pin Tab",
        onClick = onTogglePin
      )

      // 2. Duplicate Tab
      TabActionRow(
        icon = Icons.Default.ContentCopy,
        title = "Duplicate Tab",
        onClick = onDuplicate
      )

      // 3. Mark as Inactive / Sleep
      TabActionRow(
        icon = Icons.Default.Bedtime,
        title = if (tab.isInactive) "Wake / Activate Tab" else "Put Tab to Sleep (Inactive)",
        onClick = onToggleInactive
      )

      // 4. Groups Management for this tab
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "MOVE TO GROUP:",
        color = ThemeCyber.colors.textSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = CyberMonoFamily
      )

      if (tab.groupId != null) {
        TabActionRow(
          icon = Icons.Default.FolderOff,
          title = "Remove from Group",
          iconTint = ThemeCyber.colors.dangerRed,
          onClick = { onAssignToGroup(null) }
        )
      }

      tabGroups.forEach { group ->
        val isCurrent = tab.groupId == group.id
        TabActionRow(
          icon = Icons.Default.Folder,
          title = "Group: ${group.title}",
          iconTint = Color(group.colorHex),
          trailingContent = if (isCurrent) "Current" else null,
          onClick = { onAssignToGroup(group.id) }
        )
      }

      TabActionRow(
        icon = Icons.Default.CreateNewFolder,
        title = "+ Create New Group with this tab",
        iconTint = ThemeCyber.colors.secondary,
        onClick = onCreateGroupWithTab
      )
    }
  }
}

@Composable
private fun TabActionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  iconTint: Color = ThemeCyber.colors.textPrimary,
  trailingContent: String? = null,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = title,
        color = ThemeCyber.colors.textPrimary,
        fontFamily = CyberMonoFamily,
        fontSize = 13.sp
      )
    }

    if (trailingContent != null) {
      Text(
        text = trailingContent,
        color = ThemeCyber.colors.textMuted,
        fontFamily = CyberMonoFamily,
        fontSize = 10.sp
      )
    }
  }
}
