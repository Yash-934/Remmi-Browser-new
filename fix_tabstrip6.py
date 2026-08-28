import re

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'r') as f:
    content = f.read()

# Fix SpaceCard usages:
content = content.replace("""              SpaceCard(
                title = "Personal Space",
                icon = Icons.Default.Person,
                count = personalTabs.size,
                color = ThemeCyber.colors.primary,
                isSelected = selectedSpaceFilter == "personal",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "personal") null else "personal" }
              )""", """              SpaceCard(
                title = "Personal Space",
                icon = { Icon(Icons.Default.Person, contentDescription = null, tint = ThemeCyber.colors.primary, modifier = Modifier.size(16.dp)) },
                count = personalTabs.size,
                accentColor = ThemeCyber.colors.primary,
                isSelected = selectedSpaceFilter == "personal",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "personal") null else "personal" }
              )""")

content = content.replace("""              SpaceCard(
                title = "Incognito Space",
                icon = painterResource(R.drawable.ic_incognito),
                count = incognitoTabs.size,
                color = Color(0xFF8E8E93),
                isSelected = selectedSpaceFilter == "incognito",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "incognito") null else "incognito" }
              )""", """              SpaceCard(
                title = "Incognito Space",
                icon = { Icon(painterResource(R.drawable.ic_incognito), contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(16.dp)) },
                count = incognitoTabs.size,
                accentColor = Color(0xFF8E8E93),
                isSelected = selectedSpaceFilter == "incognito",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "incognito") null else "incognito" }
              )""")

content = content.replace("""              SpaceCard(
                title = "Tor Space",
                icon = Icons.Default.VpnKey,
                count = torTabs.size,
                color = ThemeCyber.colors.torPurple,
                isSelected = selectedSpaceFilter == "tor",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "tor") null else "tor" }
              )""", """              SpaceCard(
                title = "Tor Space",
                icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = ThemeCyber.colors.torPurple, modifier = Modifier.size(16.dp)) },
                count = torTabs.size,
                accentColor = ThemeCyber.colors.torPurple,
                isSelected = selectedSpaceFilter == "tor",
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == "tor") null else "tor" }
              )""")

content = content.replace("""              SpaceCard(
                title = group.title,
                icon = Icons.Default.Folder,
                count = groupTabsCount,
                color = Color(group.colorHex),
                isSelected = selectedSpaceFilter == group.id,
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == group.id) null else group.id }
              )""", """              SpaceCard(
                title = group.title,
                icon = { Icon(Icons.Default.Folder, contentDescription = null, tint = Color(group.colorHex), modifier = Modifier.size(16.dp)) },
                count = groupTabsCount,
                accentColor = Color(group.colorHex),
                isSelected = selectedSpaceFilter == group.id,
                onClick = { selectedSpaceFilter = if (selectedSpaceFilter == group.id) null else group.id }
              )""")


# Fix ModernFilterChip usages:
content = content.replace(
"""          item { ModernFilterChip("All (${tabs.size})", Icons.Default.GridView, selectedFilter == TabFilter.ALL) { selectedFilter = TabFilter.ALL } }
          item { ModernFilterChip("Recent", Icons.Default.Schedule, selectedFilter == TabFilter.RECENT) { selectedFilter = TabFilter.RECENT } }
          item { ModernFilterChip("Active (${activeOnlyTabs.size})", Icons.Default.CheckCircle, selectedFilter == TabFilter.ACTIVE) { selectedFilter = TabFilter.ACTIVE } }
          item { ModernFilterChip("Sleep (${inactiveTabs.size})", Icons.Default.NightlightRound, selectedFilter == TabFilter.INACTIVE) { selectedFilter = TabFilter.INACTIVE } }""",
"""          item { ModernFilterChip("All", count = tabs.size, icon = Icons.Default.GridView, isSelected = selectedFilter == TabFilter.ALL) { selectedFilter = TabFilter.ALL } }
          item { ModernFilterChip("Recent", icon = Icons.Default.Schedule, isSelected = selectedFilter == TabFilter.RECENT) { selectedFilter = TabFilter.RECENT } }
          item { ModernFilterChip("Active", count = activeOnlyTabs.size, icon = Icons.Default.CheckCircle, isSelected = selectedFilter == TabFilter.ACTIVE) { selectedFilter = TabFilter.ACTIVE } }
          item { ModernFilterChip("Sleep", count = inactiveTabs.size, icon = Icons.Default.NightlightRound, isSelected = selectedFilter == TabFilter.INACTIVE) { selectedFilter = TabFilter.INACTIVE } }"""
)

with open('app/src/main/java/com/remmi/browser/ui/components/TabStrip.kt', 'w') as f:
    f.write(content)

print("Fixed usages")
