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

  // Dialog States
  var showCreateGroupDialog by remember { mutableStateOf(false) }
  var editingGroup by remember { mutableStateOf<TabGroup?>(null) }
  var tabForGroupAssignment by remember { mutableStateOf<BrowserTab?>(null) }
  var tabOptionsTarget by remember { mutableStateOf<BrowserTab?>(null) }

  // Quick stats
  val totalCount = tabs.size
  val inactiveTabs = remember(tabs) { tabs.filter { it.isInactive } }
  val activeOnlyTabs = remember(tabs) { tabs.filter { !it.isInactive } }
  val ghostTabs = remember(tabs) { tabs.filter { it.profile == PrivacyProfile.GHOST } }
  val inactiveCount = inactiveTabs.size

  // Filtered Tabs according to filter chip and search query
  val filteredTabs = remember(tabs, selectedFilter, searchQuery) {
    val query = searchQuery.trim().lowercase()
    val base = when (selectedFilter) {
      TabFilter.ALL -> tabs
      TabFilter.ACTIVE -> activeOnlyTabs
      TabFilter.GROUPS -> tabs.filter { it.groupId != null }
      TabFilter.GHOST -> ghostTabs
      TabFilter.INACTIVE -> inactiveTabs
    }
    if (query.isEmpty()) {
      base
    } else {
      base.filter {
        it.title.lowercase().contains(query) || it.url.lowercase().contains(query)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(ThemeCyber.colors.background)
      .navigationBarsPadding()
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    // 1. TOP HEADER BAR
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "TABS // [${tabs.size}]",
            color = ThemeCyber.colors.primary,
            fontFamily = CyberMonoFamily,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
          )
          if (inactiveCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = ThemeCyber.colors.textMuted.copy(alpha = 0.2f),
              border = BorderStroke(0.6.dp, ThemeCyber.colors.textMuted)
            ) {
              Text(
                text = "💤 $inactiveCount INACTIVE",
                fontSize = 9.sp,
                fontFamily = CyberMonoFamily,
                fontWeight = FontWeight.Bold,
                color = ThemeCyber.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }
          }
        }
        Text(
          text = if (tabGroups.isNotEmpty()) "${tabGroups.size} Groups • Multi-Engine Active" else "Zero-Telemetry Isolated Sandbox",
          color = ThemeCyber.colors.textMuted,
          fontSize = 10.sp,
          fontFamily = CyberMonoFamily,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Create Group Button
        IconButton(
          onClick = { showCreateGroupDialog = true },
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ThemeCyber.colors.surfaceLight)
            .border(0.8.dp, ThemeCyber.colors.secondary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .testTag("new_tab_group_button"),
        ) {
          Icon(
            imageVector = Icons.Default.CreateNewFolder,
            contentDescription = "New Tab Group",
            tint = ThemeCyber.colors.secondary,
            modifier = Modifier.size(18.dp),
          )
        }

        // Add Shield Tab
        IconButton(
          onClick = {
            onNewTab(PrivacyProfile.SHIELD, null)
            onDismiss()
          },
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ThemeCyber.colors.surfaceLight)
            .border(0.8.dp, ThemeCyber.colors.primary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .testTag("add_tab_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "New Tab",
            tint = ThemeCyber.colors.primary,
            modifier = Modifier.size(18.dp),
          )
        }

        // Close Sheet
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ThemeCyber.colors.surfaceLight)
            .testTag("close_tabs_sheet_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Tabs",
            tint = ThemeCyber.colors.textPrimary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    // 2. SEARCH TABS INPUT
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = ThemeCyber.colors.surface,
      border = BorderStroke(0.8.dp, ThemeCyber.colors.surfaceBorder),
      modifier = Modifier
        .fillMaxWidth()
        .height(40.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = "Search Tabs",
          tint = ThemeCyber.colors.textMuted,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (searchQuery.isEmpty()) {
            Text(
              text = "Search tabs or domains...",
              color = ThemeCyber.colors.textMuted,
              fontFamily = CyberMonoFamily,
              fontSize = 12.sp,
            )
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = TextStyle(
              color = ThemeCyber.colors.textPrimary,
              fontFamily = CyberMonoFamily,
              fontSize = 12.5.sp,
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

    Spacer(modifier = Modifier.height(8.dp))

    // 3. FILTER CHIPS ROW
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      item {
        TabFilterChip(
          title = "All (${tabs.size})",
          isSelected = selectedFilter == TabFilter.ALL,
          onClick = { selectedFilter = TabFilter.ALL }
        )
      }
      item {
        TabFilterChip(
          title = "Active (${activeOnlyTabs.size})",
          isSelected = selectedFilter == TabFilter.ACTIVE,
          onClick = { selectedFilter = TabFilter.ACTIVE }
        )
      }
      if (tabGroups.isNotEmpty()) {
        item {
          TabFilterChip(
            title = "Groups (${tabGroups.size})",
            isSelected = selectedFilter == TabFilter.GROUPS,
            colorAccent = ThemeCyber.colors.secondary,
            onClick = { selectedFilter = TabFilter.GROUPS }
          )
        }
      }
      if (ghostTabs.isNotEmpty()) {
        item {
          TabFilterChip(
            title = "Tor (${ghostTabs.size})",
            isSelected = selectedFilter == TabFilter.GHOST,
            colorAccent = ThemeCyber.colors.torPurple,
            onClick = { selectedFilter = TabFilter.GHOST }
          )
        }
      }
      item {
        TabFilterChip(
          title = "💤 Inactive (${inactiveCount})",
          isSelected = selectedFilter == TabFilter.INACTIVE,
          colorAccent = Color(0xFF9E9E9E),
          onClick = { selectedFilter = TabFilter.INACTIVE }
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. MAIN CONTENT (GROUPS & TABS GRID)
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Inactive Banner / Bulk Action when viewing Inactive Filter
      if (selectedFilter == TabFilter.INACTIVE && inactiveTabs.isNotEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(10.dp),
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
                  text = "💤 Dormant / Inactive Tabs",
                  color = ThemeCyber.colors.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = CyberMonoFamily
                )
                Text(
                  text = "These tabs are unloaded to save battery, RAM & Tor circuits.",
                  color = ThemeCyber.colors.textMuted,
                  fontSize = 10.sp,
                  fontFamily = CyberMonoFamily
                )
              }
              Button(
                onClick = onCloseAllInactiveTabs,
                colors = ButtonDefaults.buttonColors(
                  containerColor = ThemeCyber.colors.dangerRed.copy(alpha = 0.25f),
                  contentColor = ThemeCyber.colors.dangerRed
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = "Close All",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = CyberMonoFamily
                )
              }
            }
          }
        }
      }

      // Grouped Sections (When showing ALL, ACTIVE, or GROUPS)
      if (selectedFilter != TabFilter.INACTIVE && tabGroups.isNotEmpty()) {
        val groupsToShow = if (selectedFilter == TabFilter.ACTIVE) {
          tabGroups.filter { !it.isInactive }
        } else {
          tabGroups
        }

        items(groupsToShow, key = { it.id }) { group ->
          val groupColor = Color(group.colorHex)
          val groupTabs = filteredTabs.filter { it.groupId == group.id }

          if (groupTabs.isNotEmpty() || selectedFilter == TabFilter.GROUPS) {
            TabGroupSection(
              group = group,
              groupTabs = groupTabs,
              tabs = tabs,
              activeIndex = activeIndex,
              onTabSelect = { id ->
                val idx = tabs.indexOfFirst { it.id == id }
                if (idx >= 0) {
                  onTabSelect(idx)
                  onDismiss()
                }
              },
              onTabClose = onTabClose,
              onToggleCollapse = { onToggleGroupCollapse(group.id) },
              onEditGroup = { editingGroup = group },
              onDeleteGroup = { closeTabs -> onDeleteGroup(group.id, closeTabs) },
              onNewTabInGroup = {
                onNewTab(PrivacyProfile.SHIELD, group.id)
                onDismiss()
              },
              onTabOptions = { tabOptionsTarget = it }
            )
          }
        }
      }

      // Ungrouped / Remaining Tabs Grid
      val ungroupedTabs = if (tabGroups.isNotEmpty() && selectedFilter != TabFilter.INACTIVE) {
        filteredTabs.filter { it.groupId == null }
      } else {
        filteredTabs
      }

      if (ungroupedTabs.isNotEmpty()) {
        if (tabGroups.isNotEmpty() && selectedFilter != TabFilter.INACTIVE) {
          item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            ) {
              Text(
                text = "STANDALONE TABS (${ungroupedTabs.size})",
                color = ThemeCyber.colors.textSecondary,
                fontSize = 10.5.sp,
                fontFamily = CyberMonoFamily,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        item {
          val chunkedTabs = ungroupedTabs.chunked(2)
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (row in chunkedTabs) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                for (tab in row) {
                  val originalIndex = tabs.indexOfFirst { it.id == tab.id }
                  val isActive = originalIndex == activeIndex

                  Box(modifier = Modifier.weight(1f)) {
                    EnhancedTabCard(
                      tab = tab,
                      isActive = isActive,
                      groupColor = null,
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
          }
        }
      }

      // Empty State
      if (filteredTabs.isEmpty()) {
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
                modifier = Modifier.size(42.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (searchQuery.isNotEmpty()) "No matching tabs found" else "No tabs in this section",
                color = ThemeCyber.colors.textMuted,
                fontFamily = CyberMonoFamily,
                fontSize = 13.sp
              )
            }
          }
        }
      }
    }

    // 5. BOTTOM BAR (Quick Actions: Incognito, Tor, Close All)
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Quick Incognito
        OutlinedButton(
          onClick = {
            onNewTab(PrivacyProfile.INCOGNITO, null)
            onDismiss()
          },
          border = BorderStroke(0.8.dp, ThemeCyber.colors.surfaceBorder),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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
            fontSize = 11.sp,
            color = ThemeCyber.colors.textPrimary,
            fontFamily = CyberMonoFamily
          )
        }

        // Quick Ghost / Tor
        OutlinedButton(
          onClick = {
            onNewTab(PrivacyProfile.GHOST, null)
            onDismiss()
          },
          border = BorderStroke(0.8.dp, ThemeCyber.colors.torPurple.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
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
            fontSize = 11.sp,
            color = ThemeCyber.colors.torPurple,
            fontFamily = CyberMonoFamily
          )
        }
      }

      // Close All Tabs
      TextButton(
        onClick = {
          onCloseAllTabs()
          onDismiss()
        },
        colors = ButtonDefaults.textButtonColors(contentColor = ThemeCyber.colors.dangerRed)
      ) {
        Text(
          text = "Close All",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = CyberMonoFamily
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

  // 3. Tab Options Bottom Sheet / Dialog (Pin, Sleep, Move to Group, Duplicate)
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
// TAB GROUP SECTION COMPONENT
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabGroupSection(
  group: TabGroup,
  groupTabs: List<BrowserTab>,
  tabs: List<BrowserTab>,
  activeIndex: Int,
  onTabSelect: (String) -> Unit,
  onTabClose: (String) -> Unit,
  onToggleCollapse: () -> Unit,
  onEditGroup: () -> Unit,
  onDeleteGroup: (closeTabs: Boolean) -> Unit,
  onNewTabInGroup: () -> Unit,
  onTabOptions: (BrowserTab) -> Unit,
) {
  val groupColor = Color(group.colorHex)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(ThemeCyber.colors.surface.copy(alpha = 0.9f))
      .border(1.2.dp, groupColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
      .padding(10.dp)
  ) {
    // Group Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onToggleCollapse() }
        .padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(groupColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = group.title.uppercase(),
          color = groupColor,
          fontFamily = CyberMonoFamily,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = groupColor.copy(alpha = 0.2f),
          border = BorderStroke(0.5.dp, groupColor.copy(alpha = 0.5f))
        ) {
          Text(
            text = "${groupTabs.size}",
            color = groupColor,
            fontFamily = CyberMonoFamily,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Add tab in group
        IconButton(
          onClick = onNewTabInGroup,
          modifier = Modifier.size(26.dp)
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = "Add tab to group",
            tint = groupColor,
            modifier = Modifier.size(16.dp)
          )
        }

        // Edit Group
        IconButton(
          onClick = onEditGroup,
          modifier = Modifier.size(26.dp)
        ) {
          Icon(
            Icons.Default.MoreVert,
            contentDescription = "Group Settings",
            tint = ThemeCyber.colors.textMuted,
            modifier = Modifier.size(16.dp)
          )
        }

        // Collapse / Expand Indicator
        Icon(
          imageVector = if (group.isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
          contentDescription = if (group.isCollapsed) "Expand" else "Collapse",
          tint = ThemeCyber.colors.textMuted,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // Group Tabs Grid (when not collapsed)
    AnimatedVisibility(
      visible = !group.isCollapsed,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      if (groupTabs.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Group is empty. Tap + to open tab.",
            color = ThemeCyber.colors.textMuted,
            fontSize = 11.sp,
            fontFamily = CyberMonoFamily
          )
        }
      } else {
        val chunkedTabs = groupTabs.chunked(2)
        Column(
          modifier = Modifier.padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          for (row in chunkedTabs) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              for (tab in row) {
                val originalIndex = tabs.indexOfFirst { it.id == tab.id }
                val isActive = originalIndex == activeIndex

                Box(modifier = Modifier.weight(1f)) {
                  EnhancedTabCard(
                    tab = tab,
                    isActive = isActive,
                    groupColor = groupColor,
                    onSelect = { onTabSelect(tab.id) },
                    onClose = { onTabClose(tab.id) },
                    onOptions = { onTabOptions(tab) }
                  )
                }
              }
              if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// ENHANCED TAB CARD WITH WEBSITE PREVIEW THUMBNAIL
// -------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedTabCard(
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
    PrivacyProfile.INCOGNITO -> Color(0xFFE0E0E0)
    else -> groupColor ?: ThemeCyber.colors.primary
  }

  val isBlankOrNewTab = tab.url.isEmpty() || tab.url == "about:blank" || tab.url == "netrunner://newtab"
  val cleanDomain = remember(tab.url) {
    try {
      val uri = android.net.Uri.parse(tab.url)
      uri.host?.removePrefix("www.") ?: if (isBlankOrNewTab) "netrunner:~$ home" else tab.url
    } catch (_: Exception) {
      tab.url
    }
  }

  val faviconUrl = remember(tab.url) {
    com.remmi.browser.ui.components.getFaviconUrl(tab.url)
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = ThemeCyber.colors.surface),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      width = if (isActive) 1.6.dp else 0.8.dp,
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
                profileColor.copy(alpha = 0.12f),
                ThemeCyber.colors.surface
              )
            )
          } else {
            SolidColor(ThemeCyber.colors.surface)
          }
        )
    ) {
      // 1. CARD TOP BAR (Favicon, Domain, Security, Options, Close)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(ThemeCyber.colors.surfaceLight.copy(alpha = 0.8f))
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Favicon or Incognito/Tor badge
          if (tab.profile == PrivacyProfile.INCOGNITO) {
            Icon(
              painter = painterResource(R.drawable.ic_incognito),
              contentDescription = "Incognito",
              tint = ThemeCyber.colors.textPrimary,
              modifier = Modifier.size(13.dp)
            )
          } else if (isBlankOrNewTab) {
            Icon(
              imageVector = Icons.Default.Public,
              contentDescription = null,
              tint = profileColor,
              modifier = Modifier.size(13.dp)
            )
          } else {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(faviconUrl)
                .crossfade(true)
                .build(),
              contentDescription = null,
              modifier = Modifier
                .size(14.dp)
                .clip(CircleShape),
              error = painterResource(R.drawable.remmi_logo),
              placeholder = painterResource(R.drawable.remmi_logo)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = cleanDomain,
            color = ThemeCyber.colors.textPrimary,
            fontFamily = CyberMonoFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          if (tab.isPinned) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.PushPin,
              contentDescription = "Pinned",
              tint = ThemeCyber.colors.warningYellow,
              modifier = Modifier.size(11.dp)
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Tab Options
          IconButton(
            onClick = onOptions,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = ThemeCyber.colors.textMuted,
              modifier = Modifier.size(13.dp)
            )
          }

          // Close Tab Button
          IconButton(
            onClick = onClose,
            modifier = Modifier.size(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = ThemeCyber.colors.textSecondary,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      // 2. WEBSITE PREVIEW THUMBNAIL CONTAINER
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(6.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(ThemeCyber.colors.backgroundDarker)
          .border(0.6.dp, ThemeCyber.colors.surfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
      ) {
        if (isBlankOrNewTab) {
          // New Tab / Dashboard Preview
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = profileColor.copy(alpha = 0.7f),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "NETRUNNER // HOME",
              color = profileColor,
              fontFamily = CyberMonoFamily,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Hardened Stealth Canvas",
              color = ThemeCyber.colors.textMuted,
              fontSize = 8.sp,
              fontFamily = CyberMonoFamily
            )
          }
        } else if (tab.profile == PrivacyProfile.GHOST) {
          // Tor / Ghost Preview Thumbnail
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VpnKey,
              contentDescription = null,
              tint = ThemeCyber.colors.torPurple.copy(alpha = 0.8f),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = tab.title.ifEmpty { cleanDomain },
              color = Color.White,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              textAlign = TextAlign.Center,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "3-Hop Onion Circuit",
              color = ThemeCyber.colors.torPurple,
              fontSize = 8.sp,
              fontFamily = CyberMonoFamily
            )
          }
        } else {
          // Active Website Preview Canvas Card
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            // Fake Web Title Banner
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(profileColor)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = tab.title.ifEmpty { cleanDomain },
                color = ThemeCyber.colors.textPrimary,
                fontFamily = CyberMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            // Stylized Website Visual Body (Preview Skeleton lines)
            Column(
              verticalArrangement = Arrangement.spacedBy(3.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
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
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.8f)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(ThemeCyber.colors.surfaceBorder.copy(alpha = 0.4f))
              )
            }

            // Path indicator
            Text(
              text = tab.url,
              color = ThemeCyber.colors.textMuted,
              fontFamily = CyberMonoFamily,
              fontSize = 8.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Inactive sleeping badge overlay
        if (tab.isInactive) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = BorderStroke(0.6.dp, ThemeCyber.colors.textMuted),
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
          ) {
            Text(
              text = "💤 SLEEPING",
              fontSize = 7.5.sp,
              fontFamily = CyberMonoFamily,
              color = ThemeCyber.colors.textSecondary,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }
      }

      // 3. CARD FOOTER BAR (Blocked trackers count + Active Badge)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = if (tab.blockedTrackersCount > 0) ThemeCyber.colors.secondary else ThemeCyber.colors.textMuted,
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${tab.blockedTrackersCount}",
            color = if (tab.blockedTrackersCount > 0) ThemeCyber.colors.secondary else ThemeCyber.colors.textMuted,
            fontSize = 9.sp,
            fontFamily = CyberMonoFamily,
            fontWeight = FontWeight.Bold,
          )
        }

        if (isActive) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ThemeCyber.colors.successGreen)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "ACTIVE",
              color = ThemeCyber.colors.successGreen,
              fontSize = 8.5.sp,
              fontFamily = CyberMonoFamily,
              fontWeight = FontWeight.Bold,
            )
          }
        } else {
          val timeText = remember(tab.lastAccessedAt) {
            DateUtils.getRelativeTimeSpanString(tab.lastAccessedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()
          }
          Text(
            text = timeText,
            color = ThemeCyber.colors.textMuted,
            fontSize = 8.sp,
            fontFamily = CyberMonoFamily
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// FILTER CHIP COMPONENT
// -------------------------------------------------------------

@Composable
private fun TabFilterChip(
  title: String,
  isSelected: Boolean,
  colorAccent: Color = ThemeCyber.colors.primary,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) colorAccent.copy(alpha = 0.2f) else ThemeCyber.colors.surface,
    border = BorderStroke(
      width = if (isSelected) 1.2.dp else 0.6.dp,
      color = if (isSelected) colorAccent else ThemeCyber.colors.surfaceBorder
    ),
    modifier = Modifier
      .height(28.dp)
      .clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = title,
        color = if (isSelected) colorAccent else ThemeCyber.colors.textSecondary,
        fontSize = 11.sp,
        fontFamily = CyberMonoFamily,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      )
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
