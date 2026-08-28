import re

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'r') as f:
    content = f.read()

# We need to replace everything from the broken Quick Actions Bar down to // --- DIALOGS ---
broken_start_pattern = r"        // Quick Incognito Button\n        OutlinedButton\(\n          onClick = \{\n            onNewTab\(PrivacyProfile\.INCOGNITO, null\)\n            onDismiss\(\)\n          \},\n[\s\S]*?  // --- DIALOGS ---"

missing_code = """        // Quick Incognito Button
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
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
          ) {
            item {
              SpaceCard(
                title = "Personal Space",
                icon = Icons.Default.Person,
                count = personalTabs.size,
                color = ThemeCyber.colors.primary,
                isSelected = selectedSpaceFilter == "personal",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "personal") null else "personal" }
              )
            }
            item {
              SpaceCard(
                title = "Incognito Space",
                icon = painterResource(R.drawable.ic_incognito),
                count = incognitoTabs.size,
                color = Color(0xFF8E8E93),
                isSelected = selectedSpaceFilter == "incognito",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "incognito") null else "incognito" }
              )
            }
            item {
              SpaceCard(
                title = "Tor Space",
                icon = Icons.Default.VpnKey,
                count = torTabs.size,
                color = ThemeCyber.colors.torPurple,
                isSelected = selectedSpaceFilter == "tor",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "tor") null else "tor" }
              )
            }
            items(tabGroups, key = { "group_${it.id}" }) { group ->
              val groupTabsCount = tabs.count { it.groupId == group.id }
              SpaceCard(
                title = group.title,
                icon = Icons.Default.Folder,
                count = groupTabsCount,
                color = Color(group.colorHex),
                isSelected = selectedSpaceFilter == group.id,
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == group.id) null else group.id }
              )
            }
          }
        }
      }

      // FILTERS SECTION
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
          item { ModernFilterChip("All (${tabs.size})", Icons.Default.GridView, selectedFilter == TabFilter.ALL) { selectedFilter = TabFilter.ALL } }
          item { ModernFilterChip("Recent", Icons.Default.Schedule, selectedFilter == TabFilter.RECENT) { selectedFilter = TabFilter.RECENT } }
          item { ModernFilterChip("Active (${activeOnlyTabs.size})", Icons.Default.CheckCircle, selectedFilter == TabFilter.ACTIVE) { selectedFilter = TabFilter.ACTIVE } }
          item { ModernFilterChip("Sleep (${inactiveTabs.size})", Icons.Default.NightlightRound, selectedFilter == TabFilter.INACTIVE) { selectedFilter = TabFilter.INACTIVE } }
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
  }

  // --- DIALOGS ---"""

match = re.search(broken_start_pattern, content)
if match:
    content = content.replace(match.group(0), missing_code)
    with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'w') as f:
        f.write(content)
    print("Fixed TabGridSheet completely")
else:
    print("Could not match the broken block")

