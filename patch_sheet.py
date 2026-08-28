import re

with open('app/src/main/java/com/remmi/browser/ui/components/HistoryBookmarksSheet.kt', 'r') as f:
    content = f.read()

# 1. Update HistoryBookmarksSheet signature
sig_pattern = r"fun HistoryBookmarksSheet\([\s\S]*?modifier: Modifier = Modifier,\n\) \{"
sig_match = re.search(sig_pattern, content)
if not sig_match:
    print("Could not find HistoryBookmarksSheet signature")
    exit(1)

new_sig = """fun HistoryBookmarksSheet(
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
) {"""

content = content.replace(sig_match.group(0), new_sig)

# 2. Fix HISTORY & BOOKMARKS overflow
header_pattern = r"""      Text\(\n        text = "HISTORY & BOOKMARKS",\n        color = ThemeCyber\.colors\.textPrimary,\n        fontSize = 17\.sp,\n        fontWeight = FontWeight\.Medium,\n        letterSpacing = 0\.5\.sp\n      \)"""
new_header = """      Text(
        text = "ACTIVITY",
        color = ThemeCyber.colors.textPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.weight(1f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )"""
content = content.replace(re.search(header_pattern, content).group(0), new_header)

# 3. Update ActionCard usage
action_cards_pattern = r"""    // 4 Action Cards Row\n    Row\(\n      modifier = Modifier\.fillMaxWidth\(\),\n      horizontalArrangement = Arrangement\.spacedBy\(8\.dp\)\n    \) \{\n      ActionCard\(icon = Icons\.Default\.StarBorder, title = "ADD\\nBOOKMARK", modifier = Modifier\.weight\(1f\)\)\n      ActionCard\(icon = Icons\.Default\.MenuBook, title = "SAVE TO\\nREADING LIST", modifier = Modifier\.weight\(1f\)\)\n      ActionCard\(icon = Icons\.Default\.CreateNewFolder, title = "CREATE\\nCOLLECTION", modifier = Modifier\.weight\(1f\)\)\n      ActionCard\(icon = Icons\.Default\.Sync, title = "SYNC\\nSTATUS", modifier = Modifier\.weight\(1f\)\)\n    \}"""
action_cards_match = re.search(action_cards_pattern, content)

new_action_cards = """    // 4 Action Cards Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      ActionCard(icon = Icons.Default.StarBorder, title = "ADD\\nBOOKMARK", modifier = Modifier.weight(1f), onClick = onAddBookmark)
      ActionCard(icon = Icons.Default.MenuBook, title = "SAVE TO\\nREADING LIST", modifier = Modifier.weight(1f), onClick = onSaveToReadingList)
      ActionCard(icon = Icons.Default.CreateNewFolder, title = "CREATE\\nCOLLECTION", modifier = Modifier.weight(1f), onClick = onCreateCollection)
      ActionCard(icon = Icons.Default.Sync, title = "SYNC\\nSTATUS", modifier = Modifier.weight(1f), onClick = onSyncStatus)
    }"""
if action_cards_match:
    content = content.replace(action_cards_match.group(0), new_action_cards)
else:
    print("Action cards not found")

# 4. Update HistoryItemCard usage
history_usage_pattern = r"""              HistoryItemCard\(\n                item = item,\n                onItemClick = \{\n                  onSelectUrl\(item\.url\)\n                  onDismiss\(\)\n                \},\n                onDelete = \{ onDeleteHistory\(item\) \}\n              \)"""
new_history_usage = """              HistoryItemCard(
                item = item,
                onItemClick = {
                  onSelectUrl(item.url)
                  onDismiss()
                },
                onDelete = { onDeleteHistory(item) },
                onBookmark = { onAddHistoryItemToBookmark(item) },
                onShare = { onShareUrl(item.url) }
              )"""
history_usage_match = re.search(history_usage_pattern, content)
if history_usage_match:
    content = content.replace(history_usage_match.group(0), new_history_usage)
else:
    print("HistoryItemCard usage not found")

# 5. Update HistoryItemCard signature
item_sig_pattern = r"""@Composable\nprivate fun HistoryItemCard\(\n  item: HistoryItem,\n  onItemClick: \(\) -> Unit,\n  onDelete: \(\) -> Unit,\n\) \{"""
new_item_sig = """@Composable
private fun HistoryItemCard(
  item: HistoryItem,
  onItemClick: () -> Unit,
  onDelete: () -> Unit,
  onBookmark: () -> Unit = {},
  onShare: () -> Unit = {},
) {"""
item_sig_match = re.search(item_sig_pattern, content)
if item_sig_match:
    content = content.replace(item_sig_match.group(0), new_item_sig)

# 6. Update HistoryItemCard icons
icons_pattern = r"""      // Action Icons Column\n      Column\(\n        horizontalAlignment = Alignment\.CenterHorizontally,\n        verticalArrangement = Arrangement\.spacedBy\(10\.dp\)\n      \) \{\n        Icon\(Icons\.Default\.StarBorder, contentDescription = "Star", tint = ThemeCyber\.colors\.textSecondary, modifier = Modifier\.size\(18\.dp\)\)\n        Icon\(Icons\.Default\.Delete, contentDescription = "Delete", tint = ThemeCyber\.colors\.textSecondary, modifier = Modifier\.size\(18\.dp\)\.clickable \{ onDelete\(\) \}\)\n        Icon\(Icons\.Default\.Share, contentDescription = "Share", tint = ThemeCyber\.colors\.textSecondary, modifier = Modifier\.size\(18\.dp\)\)\n      \}"""
new_icons = """      // Action Icons Column
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(Icons.Default.StarBorder, contentDescription = "Star", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onBookmark() })
        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onDelete() })
        Icon(Icons.Default.Share, contentDescription = "Share", tint = ThemeCyber.colors.textSecondary, modifier = Modifier.size(18.dp).clickable { onShare() })
      }"""
icons_match = re.search(icons_pattern, content)
if icons_match:
    content = content.replace(icons_match.group(0), new_icons)
else:
    print("Action icons in HistoryItemCard not found")

# 7. Update ActionCard signature
action_card_sig_pattern = r"""private fun ActionCard\(icon: androidx\.compose\.ui\.graphics\.vector\.ImageVector, title: String, modifier: Modifier\) \{"""
new_action_card_sig = """private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, modifier: Modifier, onClick: () -> Unit = {}) {"""
content = content.replace(re.search(action_card_sig_pattern, content).group(0), new_action_card_sig)

# Add clickable to ActionCard Surface
surface_pattern = r"""  Surface\(\n    shape = RoundedCornerShape\(12\.dp\),\n    color = ThemeCyber\.colors\.surface,\n    border = BorderStroke\(1\.dp, ThemeCyber\.colors\.surfaceBorder\),\n    modifier = modifier\.aspectRatio\(1f\)\n  \)"""
new_surface = """  Surface(
    shape = RoundedCornerShape(12.dp),
    color = ThemeCyber.colors.surface,
    border = BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder),
    modifier = modifier.aspectRatio(1f).clickable { onClick() }
  )"""
content = content.replace(re.search(surface_pattern, content).group(0), new_surface)

with open('app/src/main/java/com/remmi/browser/ui/components/HistoryBookmarksSheet.kt', 'w') as f:
    f.write(content)

print("Success")

