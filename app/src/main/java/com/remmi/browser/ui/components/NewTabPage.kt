package com.remmi.browser.ui.components

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.remmi.browser.util.DefaultBrowserHelper
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.remmi.browser.R
import com.remmi.browser.security.ClipboardManager
import com.remmi.browser.security.NetworkHardening
import com.remmi.browser.security.PrivacyProfile
import com.remmi.browser.security.TorCircuit
import com.remmi.browser.security.TorManager
import com.remmi.browser.storage.BookmarkItem
import com.remmi.browser.storage.HistoryItem
import com.remmi.browser.storage.NetRunnerDatabase
import com.remmi.browser.storage.SearchEngine
import com.remmi.browser.storage.SpeedDialItem
import com.remmi.browser.ui.theme.CyberMonoFamily
import com.remmi.browser.ui.theme.CyberTheme
import com.remmi.browser.ui.theme.ThemeCyber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun getFaviconUrl(url: String): String {
  if (com.remmi.browser.security.NetworkRouteAuthority.isOnionDestination(url)) {
    return "" // Zero external clearnet leaks for Tor hidden services
  }
  return try {
    val cleanDomain = if (url.startsWith("http://") || url.startsWith("https://")) {
      android.net.Uri.parse(url).host ?: url
    } else {
      url.split("/").firstOrNull() ?: url
    }
    "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128"
  } catch (e: Exception) {
    "https://www.google.com/s2/favicons?domain=google.com&sz=128"
  }
}

@Composable
fun NewTabPage(
  profile: PrivacyProfile,
  blockedTrackersCount: Int,
  torState: TorManager.TorState = TorManager.TorState.OFF,
  circuit: TorCircuit? = null,
  isDesktopMode: Boolean = false,
  isReaderMode: Boolean = false,
  searchEngine: SearchEngine = SearchEngine.DUCK_DUCK_GO,
  speedDials: List<SpeedDialItem> = emptyList(),
  backgroundAnimation: String = BackgroundTypes.CYBERPUNK_GRID,
  customWallpaperUri: String? = null,
  wallpaperDimLevel: Float = 0.0f,
  fullscreenWallpaperEnabled: Boolean = true,
  wallpaperScaleMode: String = "CROP",
  onSearch: (query: String, engine: SearchEngine) -> Unit = { _, _ -> },
  onNavigate: (String) -> Unit = {},
  onSelectSearchEngine: (SearchEngine) -> Unit = {},
  onAddSpeedDial: (SpeedDialItem) -> Unit = {},
  onEditSpeedDial: (SpeedDialItem) -> Unit = {},
  onDeleteSpeedDial: (String) -> Unit = {},
  onResetSpeedDials: () -> Unit = {},
  onUpdateWallpaper: (String?) -> Unit = {},
  onUpdateBackgroundAnimation: (String) -> Unit = {},
  onUpdateWallpaperDimLevel: (Float) -> Unit = {},
  onUpdateFullscreenWallpaper: (Boolean) -> Unit = {},
  onUpdateWallpaperScaleMode: (String) -> Unit = {},
  onNewTab: () -> Unit = {},
  onOpenBookmarks: () -> Unit = {},
  onOpenHistory: () -> Unit = {},
  onOpenDownloads: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
  onToggleDesktop: () -> Unit = {},
  onToggleGhost: () -> Unit = {},
  onToggleReader: () -> Unit = {},
  onInspectCircuit: () -> Unit = {},
  onSecurityShieldClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val clipboard = remember { ClipboardManager(context) }
  val database = remember { NetRunnerDatabase.getDatabase(context) }
  val activity = context as? Activity
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var showWallpaperDialog by remember { mutableStateOf(false) }
  var showSearchEngineMenu by remember { mutableStateOf(false) }
  var selectedItemForAction by remember { mutableStateOf<SpeedDialItem?>(null) }
  var editingItem by remember { mutableStateOf<SpeedDialItem?>(null) }
  var itemToDelete by remember { mutableStateOf<SpeedDialItem?>(null) }
  var newTitle by remember { mutableStateOf("") }
  var newUrl by remember { mutableStateOf("") }

  var copiedUrlPrompt by remember { mutableStateOf<String?>(null) }
  var dismissedCopiedUrl by remember { mutableStateOf<String?>(null) }
  var historySuggestions by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
  var bookmarkSuggestions by remember { mutableStateOf<List<BookmarkItem>>(emptyList()) }

  // Check clipboard on resume / enter
  LaunchedEffect(Unit) {
    val clip = clipboard.getCopiedUrl()
    if (!clip.isNullOrBlank() && clip != dismissedCopiedUrl) {
      copiedUrlPrompt = clip
    }
  }

  // Live Query Suggestions for History & Bookmarks
  LaunchedEffect(searchQuery) {
    val query = searchQuery.trim()
    if (query.isNotEmpty()) {
      withContext(Dispatchers.IO) {
        val hist = database.historyDao().searchHistory(query)
        val bkmk = database.bookmarkDao().searchBookmarks(query)
        historySuggestions = hist
        bookmarkSuggestions = bkmk
      }
    } else {
      historySuggestions = emptyList()
      bookmarkSuggestions = emptyList()
    }
  }

  var isDefaultBrowser by remember {
    mutableStateOf(DefaultBrowserHelper.isDefaultBrowser(context))
  }
  var hasDismissedDefaultBanner by remember { mutableStateOf(false) }

  val defaultRoleLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    isDefaultBrowser = DefaultBrowserHelper.isDefaultBrowser(context)
  }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      onUpdateWallpaper(uri.toString())
      onUpdateBackgroundAnimation(BackgroundTypes.CUSTOM_IMAGE)
    }
  }

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    // 1. Dynamic Cyberpunk Background Animation / Custom Image (rendered locally if not in root full screen mode)
    if (!fullscreenWallpaperEnabled) {
      CyberpunkBackground(
        backgroundType = backgroundAnimation,
        customWallpaperUri = customWallpaperUri,
        wallpaperDimLevel = wallpaperDimLevel,
        wallpaperScaleMode = wallpaperScaleMode,
      )
    }

    val hasCustomWallpaper = customWallpaperUri != null
    val isLight = ThemeCyber.colors.isLight

    val textShadow = if (hasCustomWallpaper) {
      androidx.compose.ui.graphics.Shadow(
        color = Color.Black.copy(alpha = 0.85f),
        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
        blurRadius = 6f
      )
    } else {
      null
    }

    val primaryTextColor = if (hasCustomWallpaper) Color.White else ThemeCyber.colors.textPrimary
    val secondaryTextColor = if (hasCustomWallpaper) Color.White.copy(alpha = 0.8f) else ThemeCyber.colors.textSecondary

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 18.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Status & Quick Customization Header (Edge Browser Style)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Profile / Ghost mode badge
        val badgeBg = when {
          profile == PrivacyProfile.GHOST -> if (hasCustomWallpaper) ThemeCyber.colors.torPurple.copy(alpha = 0.85f) else ThemeCyber.colors.torPurple.copy(alpha = 0.15f)
          hasCustomWallpaper -> Color.Black.copy(alpha = 0.35f)
          isLight -> ThemeCyber.colors.surface
          else -> ThemeCyber.colors.surface
        }
        val badgeBorderColor = when {
          profile == PrivacyProfile.GHOST -> ThemeCyber.colors.torPurple
          hasCustomWallpaper -> Color.White.copy(alpha = 0.3f)
          else -> ThemeCyber.colors.surfaceBorder
        }
        val badgeContentColor = when {
          profile == PrivacyProfile.GHOST -> if (hasCustomWallpaper) Color.White else ThemeCyber.colors.torPurple
          hasCustomWallpaper -> Color.White
          else -> ThemeCyber.colors.textPrimary
        }

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = badgeBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, badgeBorderColor),
          shadowElevation = if (!hasCustomWallpaper && isLight) 1.dp else 0.dp,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggleGhost() }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = if (profile == PrivacyProfile.GHOST) Icons.Default.VpnKey else Icons.Default.Shield,
              contentDescription = "Profile",
              tint = if (profile == PrivacyProfile.GHOST) (if (hasCustomWallpaper) Color.White else ThemeCyber.colors.torPurple) else ThemeCyber.colors.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = if (profile == PrivacyProfile.GHOST) "GHOST TOR" else "SECURE",
              color = badgeContentColor,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = ThemeCyber.fontFamily,
              style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
            )
          }
        }

        // Wallpaper / Settings Quick Buttons
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val iconBtnBg = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.35f) else ThemeCyber.colors.surface
          val iconBtnBorder = if (hasCustomWallpaper) Color.White.copy(alpha = 0.3f) else ThemeCyber.colors.surfaceBorder
          val iconBtnTint = if (hasCustomWallpaper) Color.White else ThemeCyber.colors.textPrimary

          Surface(
            shape = CircleShape,
            color = iconBtnBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, iconBtnBorder),
            shadowElevation = if (!hasCustomWallpaper && isLight) 1.dp else 0.dp,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .clickable { showWallpaperDialog = true }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Palette,
                contentDescription = "Wallpaper",
                tint = iconBtnTint,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = iconBtnBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, iconBtnBorder),
            shadowElevation = if (!hasCustomWallpaper && isLight) 1.dp else 0.dp,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .clickable { onOpenSettings() }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = iconBtnTint,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Vertical spacer to center the central brand & shortcuts group
      Spacer(modifier = Modifier.weight(0.7f))

      // Remmi Browser Central Brand Hero
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 18.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          shadowElevation = 6.dp,
          color = Color.Transparent
        ) {
          Image(
            painter = painterResource(id = R.drawable.remmi_logo),
            contentDescription = "Remmi Browser Logo",
            modifier = Modifier
              .size(64.dp)
              .clip(RoundedCornerShape(20.dp))
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "REMMI BROWSER",
          color = primaryTextColor,
          fontSize = 19.sp,
          fontWeight = FontWeight.ExtraBold,
          fontFamily = ThemeCyber.fontFamily,
          letterSpacing = 2.sp,
          style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
        )
      }

      // Search Bar (Edge style elegant capsule with glass effect)
      val searchBg = if (hasCustomWallpaper) Color.White.copy(alpha = 0.94f) else ThemeCyber.colors.surface
      val searchBorder = if (hasCustomWallpaper) Color.White.copy(alpha = 0.6f) else ThemeCyber.colors.surfaceBorder
      val searchTextColor = if (hasCustomWallpaper) Color.Black else ThemeCyber.colors.textPrimary
      val searchPlaceholderColor = if (hasCustomWallpaper) Color(0xFF6E6E73) else ThemeCyber.colors.textMuted
      val searchIconTint = ThemeCyber.colors.primary

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = if (isLight) 3.dp else 1.dp,
        color = searchBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, searchBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box {
            IconButton(
              onClick = { showSearchEngineMenu = true },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Switch Search Engine (${searchEngine.displayName})",
                tint = searchIconTint,
                modifier = Modifier.size(20.dp)
              )
            }

            DropdownMenu(
              expanded = showSearchEngineMenu,
              onDismissRequest = { showSearchEngineMenu = false },
              modifier = Modifier
                .background(ThemeCyber.colors.surface)
                .border(1.dp, ThemeCyber.colors.surfaceBorder, RoundedCornerShape(8.dp))
            ) {
              com.remmi.browser.storage.SearchEngine.entries.forEach { engine ->
                val isSelected = searchEngine == engine
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(
                        text = engine.displayName,
                        color = if (isSelected) ThemeCyber.colors.primary else ThemeCyber.colors.textPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                      )
                      Text(
                        text = engine.subtitle,
                        color = ThemeCyber.colors.textSecondary,
                        fontSize = 10.sp
                      )
                    }
                  },
                  onClick = {
                    onSelectSearchEngine(engine)
                    showSearchEngineMenu = false
                  },
                  leadingIcon = if (isSelected) {
                    {
                      Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = ThemeCyber.colors.primary,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  } else null
                )
              }
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          androidx.compose.foundation.text.BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
              color = searchTextColor,
              fontSize = 15.5.sp,
              fontWeight = FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
              focusManager.clearFocus()
              if (searchQuery.isNotBlank()) {
                val trimmed = searchQuery.trim()
                if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || (trimmed.contains(".") && !trimmed.contains(" "))) {
                  onNavigate(NetworkHardening.sanitizeUrl(trimmed))
                } else {
                  onSearch(trimmed, searchEngine)
                }
              }
            }),
            decorationBox = { innerTextField ->
              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (searchQuery.isEmpty()) {
                  Text(
                    text = "Search or enter web address",
                    color = searchPlaceholderColor,
                    fontSize = 15.sp
                  )
                }
                innerTextField()
              }
            },
            modifier = Modifier.weight(1f)
          )
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { searchQuery = "" },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Clear", tint = searchPlaceholderColor, modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      // Copied Link Prompt on New Tab Page
      AnimatedVisibility(
        visible = copiedUrlPrompt != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
      ) {
        copiedUrlPrompt?.let { copied ->
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.8f) else ThemeCyber.colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.primary.copy(alpha = 0.7f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp, start = 4.dp, end = 4.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onNavigate(NetworkHardening.sanitizeUrl(copied))
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(ThemeCyber.colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.ContentPaste,
                  contentDescription = "Copied Link",
                  tint = ThemeCyber.colors.primary,
                  modifier = Modifier.size(18.dp)
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Link you copied",
                  color = ThemeCyber.colors.primary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = CyberMonoFamily
                )
                Text(
                  text = copied,
                  color = Color.White,
                  fontSize = 12.5.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Spacer(modifier = Modifier.width(6.dp))

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = ThemeCyber.colors.primary.copy(alpha = 0.25f),
                modifier = Modifier.clickable { onNavigate(NetworkHardening.sanitizeUrl(copied)) }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Go",
                    color = ThemeCyber.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = CyberMonoFamily
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ThemeCyber.colors.primary,
                    modifier = Modifier.size(12.dp)
                  )
                }
              }

              IconButton(
                onClick = {
                  dismissedCopiedUrl = copied
                  copiedUrlPrompt = null
                },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Dismiss",
                  tint = Color.White.copy(alpha = 0.6f),
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }

      // Live Suggestions dropdown when typing in search bar on Home Page
      AnimatedVisibility(
        visible = searchQuery.isNotBlank() && (historySuggestions.isNotEmpty() || bookmarkSuggestions.isNotEmpty()),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
      ) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.85f) else ThemeCyber.colors.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder.copy(alpha = 0.8f)),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp)
            .padding(top = 10.dp, start = 4.dp, end = 4.dp)
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp)
          ) {
            // Bookmarks
            if (bookmarkSuggestions.isNotEmpty()) {
              items(bookmarkSuggestions.take(3), key = { "ntp_bkmk_${it.id}" }) { bkmk ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(bkmk.url) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = ThemeCyber.colors.warningYellow,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = bkmk.title.ifEmpty { bkmk.url },
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = bkmk.url,
                      color = Color.White.copy(alpha = 0.6f),
                      fontSize = 11.sp,
                      fontFamily = CyberMonoFamily,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  IconButton(
                    onClick = { searchQuery = bkmk.url },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      Icons.Default.NorthWest,
                      contentDescription = "Fill",
                      tint = Color.White.copy(alpha = 0.6f),
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }
              }
            }

            // History
            if (historySuggestions.isNotEmpty()) {
              items(historySuggestions.take(6), key = { "ntp_hist_${it.id}" }) { hist ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(hist.url) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = ThemeCyber.colors.primary,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = hist.title.ifEmpty { hist.url },
                      color = Color.White,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = hist.url,
                      color = Color.White.copy(alpha = 0.6f),
                      fontSize = 11.sp,
                      fontFamily = CyberMonoFamily,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                  IconButton(
                    onClick = { searchQuery = hist.url },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      Icons.Default.NorthWest,
                      contentDescription = "Fill",
                      tint = Color.White.copy(alpha = 0.6f),
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Speed Dials Grid (Websites with Logos)
      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
      ) {
        items(speedDials.take(11), key = { it.id }) { item ->
          SpeedDialIcon(
            item = item,
            hasCustomWallpaper = hasCustomWallpaper,
            onClick = { onNavigate(item.url) },
            onLongClick = { selectedItemForAction = item }
          )
        }
        item {
          SpeedDialAddButton(
            hasCustomWallpaper = hasCustomWallpaper,
            onClick = {
              newTitle = ""
              newUrl = ""
              showAddDialog = true
            }
          )
        }
      }

      // Bottom spacer to balance vertical centering
      Spacer(modifier = Modifier.weight(1f))

      // Default Browser Prompt Banner (if not default and not dismissed)
      if (!isDefaultBrowser && !hasDismissedDefaultBanner && activity != null) {
        val bannerBg = if (hasCustomWallpaper) Color.Black.copy(alpha = 0.65f) else ThemeCyber.colors.surface
        val bannerBorder = if (hasCustomWallpaper) Color.White.copy(alpha = 0.25f) else ThemeCyber.colors.surfaceBorder

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp)),
          color = bannerBg,
          border = androidx.compose.foundation.BorderStroke(1.dp, bannerBorder),
          shadowElevation = if (!hasCustomWallpaper && isLight) 2.dp else 0.dp,
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = ThemeCyber.colors.primary,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Set as default browser?",
                  color = primaryTextColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  style = androidx.compose.ui.text.TextStyle(shadow = textShadow)
                )
                Text(
                  text = "Open links securely with Tor & AdBlock",
                  color = secondaryTextColor,
                  fontSize = 10.sp
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Button(
                onClick = {
                  DefaultBrowserHelper.requestSetDefaultBrowser(activity, defaultRoleLauncher)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Text("SET", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = ThemeCyber.fontFamily)
              }
              Spacer(modifier = Modifier.width(4.dp))
              IconButton(
                onClick = { hasDismissedDefaultBanner = true },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Dismiss",
                  tint = secondaryTextColor,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // Wallpaper & Background Animation Customizer Dialog
  if (showWallpaperDialog) {
    AlertDialog(
      onDismissRequest = { showWallpaperDialog = false },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Palette, null, tint = ThemeCyber.colors.primary)
          Text(
            "WALLPAPER & ANIMATIONS",
            color = ThemeCyber.colors.primary,
            fontFamily = ThemeCyber.fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            "Live Background Animation",
            color = ThemeCyber.colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )

          val animations = listOf(
            BackgroundTypes.LIGHT_AURA_MESH to "Ambient Aura Waves (Light Mode)",
            BackgroundTypes.LIGHT_FLOATING_ORBS to "Floating Pastel Orbs (Light Mode)",
            BackgroundTypes.LIGHT_GEOMETRIC_DOTS to "Minimal Pulsing Dot Grid (Light Mode)",
            BackgroundTypes.LIGHT_CONSTELLATION to "Connected Nodes Constellation",
            BackgroundTypes.CYBERPUNK_GRID to "Cyberpunk 3D Grid (Retro)",
            BackgroundTypes.MATRIX_RAIN to "Matrix Digital Rain",
            BackgroundTypes.NEON_PARTICLES to "Neon Quantum Particles",
            BackgroundTypes.DIGITAL_AURORA to "Digital Neon Aurora",
            BackgroundTypes.MINIMAL_GRADIENT to "Minimal Stealth Gradient",
          )

          animations.forEach { (type, label) ->
            val isSelected = backgroundAnimation == type && customWallpaperUri == null
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  onUpdateBackgroundAnimation(type)
                  onUpdateWallpaper(null)
                  showWallpaperDialog = false
                },
              color = if (isSelected) ThemeCyber.colors.primary.copy(alpha = 0.15f) else ThemeCyber.colors.background,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) ThemeCyber.colors.primary else ThemeCyber.colors.surfaceBorder
              ),
              shape = RoundedCornerShape(10.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = label,
                  color = if (isSelected) ThemeCyber.colors.primary else ThemeCyber.colors.textPrimary,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (isSelected) {
                  Icon(Icons.Default.Check, null, tint = ThemeCyber.colors.primary, modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          Divider(modifier = Modifier.padding(vertical = 6.dp), color = ThemeCyber.colors.surfaceBorder)

          Text(
            "Custom Wallpaper (Photo)",
            color = ThemeCyber.colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )

          // Gallery Pick Button
          Button(
            onClick = {
              photoPickerLauncher.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.primary),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = if (ThemeCyber.colors.isLight) Color.White else Color.Black, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Photo from Gallery", color = if (ThemeCyber.colors.isLight) Color.White else Color.Black, fontWeight = FontWeight.Bold)
          }

          if (customWallpaperUri != null) {
            // Full Visibility & Dimming Controls
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = ThemeCyber.colors.background,
              border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder)
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    "Wallpaper Visibility",
                    color = ThemeCyber.colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                  val visibilityPercent = ((1f - wallpaperDimLevel) * 100).toInt()
                  Text(
                    if (wallpaperDimLevel <= 0.01f) "100% Full Visibility" else "$visibilityPercent% Visible",
                    color = if (wallpaperDimLevel <= 0.01f) ThemeCyber.colors.neonCyan else ThemeCyber.colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                // Presets: 100% Full Visibility, 75%, 50%, 25%
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  val presets = listOf(
                    0.0f to "100% Full",
                    0.25f to "75%",
                    0.50f to "50%",
                    0.75f to "25%"
                  )
                  presets.forEach { (dimVal, label) ->
                    val isPresetSelected = Math.abs(wallpaperDimLevel - dimVal) < 0.05f
                    Surface(
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onUpdateWallpaperDimLevel(dimVal) },
                      shape = RoundedCornerShape(6.dp),
                      color = if (isPresetSelected) ThemeCyber.colors.primary.copy(alpha = 0.2f) else ThemeCyber.colors.surface,
                      border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPresetSelected) ThemeCyber.colors.primary else ThemeCyber.colors.surfaceBorder
                      )
                    ) {
                      Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(
                          label,
                          color = if (isPresetSelected) ThemeCyber.colors.primary else ThemeCyber.colors.textSecondary,
                          fontSize = 10.sp,
                          fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal
                        )
                      }
                    }
                  }
                }

                // Continuous Visibility Slider
                Slider(
                  value = 1f - wallpaperDimLevel,
                  onValueChange = { visibility ->
                    onUpdateWallpaperDimLevel(1f - visibility)
                  },
                  colors = SliderDefaults.colors(
                    thumbColor = ThemeCyber.colors.primary,
                    activeTrackColor = ThemeCyber.colors.primary,
                    inactiveTrackColor = ThemeCyber.colors.surfaceBorder
                  ),
                  modifier = Modifier.fillMaxWidth().height(28.dp)
                )
              }
            }

            // Full Screen Edge-to-Edge Switch
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = ThemeCyber.colors.background,
              border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "Full Screen Background",
                    color = ThemeCyber.colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    "Spans behind top search bar & bottom toolbar",
                    color = ThemeCyber.colors.textMuted,
                    fontSize = 10.sp
                  )
                }
                Switch(
                  checked = fullscreenWallpaperEnabled,
                  onCheckedChange = { onUpdateFullscreenWallpaper(it) },
                  colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = ThemeCyber.colors.primary,
                    uncheckedThumbColor = ThemeCyber.colors.textMuted,
                    uncheckedTrackColor = ThemeCyber.colors.surface
                  )
                )
              }
            }

            // Scaling Fit Mode Chips
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              color = ThemeCyber.colors.background,
              border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.surfaceBorder)
            ) {
              Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  "Image Scaling / Fit",
                  color = ThemeCyber.colors.textPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  listOf(
                    "CROP" to "Fill (Crop)",
                    "FIT" to "Fit Image",
                    "FILL" to "Stretch"
                  ).forEach { (mode, label) ->
                    val isSelected = wallpaperScaleMode.equals(mode, ignoreCase = true)
                    Surface(
                      modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onUpdateWallpaperScaleMode(mode) },
                      shape = RoundedCornerShape(6.dp),
                      color = if (isSelected) ThemeCyber.colors.primary.copy(alpha = 0.2f) else ThemeCyber.colors.surface,
                      border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) ThemeCyber.colors.primary else ThemeCyber.colors.surfaceBorder
                      )
                    ) {
                      Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(
                          label,
                          color = if (isSelected) ThemeCyber.colors.primary else ThemeCyber.colors.textSecondary,
                          fontSize = 10.sp,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                      }
                    }
                  }
                }
              }
            }

            OutlinedButton(
              onClick = {
                onUpdateWallpaper(null)
                onUpdateBackgroundAnimation(BackgroundTypes.CYBERPUNK_GRID)
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, ThemeCyber.colors.dangerRed.copy(alpha = 0.6f))
            ) {
              Icon(Icons.Default.Delete, null, tint = ThemeCyber.colors.dangerRed, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Remove Custom Wallpaper", color = ThemeCyber.colors.dangerRed, fontSize = 12.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showWallpaperDialog = false }) {
          Text("Done", color = ThemeCyber.colors.primary)
        }
      },
      containerColor = ThemeCyber.colors.surface,
    )
  }

  // Add Speed Dial Shortcut Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = {
        Text("Add Website Shortcut", color = ThemeCyber.colors.textPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Name (e.g. YouTube)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newUrl,
            onValueChange = { newUrl = it },
            label = { Text("URL (e.g. youtube.com)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newUrl.isNotBlank()) {
              val formattedUrl = if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                "https://$newUrl"
              } else newUrl
              val title = newTitle.ifBlank { formattedUrl.substringAfter("://").substringBefore("/") }
              onAddSpeedDial(SpeedDialItem(title = title, url = formattedUrl))
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.primary)
        ) {
          Text("Add", color = Color.Black)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("Cancel", color = ThemeCyber.colors.textMuted)
        }
      },
      containerColor = ThemeCyber.colors.surface,
    )
  }

  // Shortcut Options Action Dialog (Open / Edit / Delete)
  selectedItemForAction?.let { item ->
    AlertDialog(
      onDismissRequest = { selectedItemForAction = null },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ThemeCyber.colors.surfaceLight,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = item.title.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = ThemeCyber.colors.primary,
                fontSize = 16.sp
              )
            }
          }
          Column {
            Text(
              text = item.title,
              color = ThemeCyber.colors.textPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = item.url,
              color = ThemeCyber.colors.textSecondary,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Open
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = ThemeCyber.colors.surfaceLight.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                val targetUrl = item.url
                selectedItemForAction = null
                onNavigate(targetUrl)
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = ThemeCyber.colors.primary, modifier = Modifier.size(20.dp))
              Text("Open Website", color = ThemeCyber.colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
          }

          // Edit
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = ThemeCyber.colors.surfaceLight.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                editingItem = item
                selectedItemForAction = null
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ThemeCyber.colors.neonCyan, modifier = Modifier.size(20.dp))
              Text("Edit Shortcut", color = ThemeCyber.colors.textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
          }

          // Delete
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = ThemeCyber.colors.dangerRed.copy(alpha = 0.12f),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable {
                itemToDelete = item
                selectedItemForAction = null
              }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThemeCyber.colors.dangerRed, modifier = Modifier.size(20.dp))
              Text("Delete Shortcut", color = ThemeCyber.colors.dangerRed, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { selectedItemForAction = null }) {
          Text("Cancel", color = ThemeCyber.colors.textMuted)
        }
      },
      containerColor = ThemeCyber.colors.surface,
    )
  }

  // Edit Speed Dial Dialog
  editingItem?.let { item ->
    var editTitle by remember(item.id) { mutableStateOf(item.title) }
    var editUrl by remember(item.id) { mutableStateOf(item.url) }

    AlertDialog(
      onDismissRequest = { editingItem = null },
      title = {
        Text("Edit Shortcut", color = ThemeCyber.colors.textPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = editTitle,
            onValueChange = { editTitle = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = editUrl,
            onValueChange = { editUrl = it },
            label = { Text("URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editUrl.isNotBlank()) {
              val formattedUrl = if (!editUrl.startsWith("http://") && !editUrl.startsWith("https://")) {
                "https://$editUrl"
              } else editUrl
              val title = editTitle.ifBlank { formattedUrl.substringAfter("://").substringBefore("/") }
              onEditSpeedDial(item.copy(title = title, url = formattedUrl))
              editingItem = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.primary)
        ) {
          Text("Save", color = Color.Black)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingItem = null }) {
          Text("Cancel", color = ThemeCyber.colors.textMuted)
        }
      },
      containerColor = ThemeCyber.colors.surface,
    )
  }

  // Delete Confirmation Dialog
  itemToDelete?.let { item ->
    AlertDialog(
      onDismissRequest = { itemToDelete = null },
      title = {
        Text("Delete Shortcut?", color = ThemeCyber.colors.textPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "Are you sure you want to remove \"${item.title}\" from your home screen shortcuts?",
          color = ThemeCyber.colors.textSecondary,
          fontSize = 14.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteSpeedDial(item.id)
            itemToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ThemeCyber.colors.dangerRed)
        ) {
          Text("Delete", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { itemToDelete = null }) {
          Text("Cancel", color = ThemeCyber.colors.textMuted)
        }
      },
      containerColor = ThemeCyber.colors.surface,
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialIcon(
  item: SpeedDialItem,
  hasCustomWallpaper: Boolean = false,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {}
) {
  val context = LocalContext.current
  var isImageError by remember(item.url) { mutableStateOf(false) }
  val faviconUrl = remember(item.url) { getFaviconUrl(item.url) }
  val isLight = ThemeCyber.colors.isLight

  val textShadow = if (hasCustomWallpaper) {
    androidx.compose.ui.graphics.Shadow(
      color = Color.Black.copy(alpha = 0.9f),
      offset = androidx.compose.ui.geometry.Offset(0f, 2f),
      blurRadius = 6f
    )
  } else null

  val cardBg = if (hasCustomWallpaper) Color.White.copy(alpha = 0.94f) else if (isLight) Color.White else ThemeCyber.colors.surfaceLight
  val cardBorder = if (hasCustomWallpaper) Color.White.copy(alpha = 0.6f) else ThemeCyber.colors.surfaceBorder.copy(alpha = if (isLight) 0.8f else 0.5f)
  val labelColor = if (hasCustomWallpaper) Color.White else ThemeCyber.colors.textPrimary

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .padding(vertical = 4.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = cardBg,
      shadowElevation = if (isLight) 2.dp else 1.dp,
      modifier = Modifier
        .size(56.dp)
        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .padding(10.dp)
      ) {
        if (!isImageError) {
          AsyncImage(
            model = ImageRequest.Builder(context)
              .data(faviconUrl)
              .crossfade(true)
              .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(6.dp)),
            onError = { isImageError = true },
          )
        } else {
          Text(
            text = item.title.firstOrNull()?.uppercase() ?: "?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ThemeCyber.colors.primary,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = item.title,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      color = labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      style = androidx.compose.ui.text.TextStyle(shadow = textShadow),
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun SpeedDialAddButton(
  hasCustomWallpaper: Boolean = false,
  onClick: () -> Unit
) {
  val isLight = ThemeCyber.colors.isLight

  val textShadow = if (hasCustomWallpaper) {
    androidx.compose.ui.graphics.Shadow(
      color = Color.Black.copy(alpha = 0.9f),
      offset = androidx.compose.ui.geometry.Offset(0f, 2f),
      blurRadius = 6f
    )
  } else null

  val cardBg = if (hasCustomWallpaper) Color.White.copy(alpha = 0.88f) else if (isLight) Color(0xFFF1F3F4) else ThemeCyber.colors.surfaceLight
  val cardBorder = if (hasCustomWallpaper) Color.White.copy(alpha = 0.6f) else ThemeCyber.colors.surfaceBorder.copy(alpha = if (isLight) 0.8f else 0.5f)
  val iconTint = if (hasCustomWallpaper) Color(0xFF2C2C2E) else ThemeCyber.colors.textPrimary
  val labelColor = if (hasCustomWallpaper) Color.White else ThemeCyber.colors.textSecondary

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = cardBg,
      shadowElevation = if (isLight) 1.dp else 0.dp,
      modifier = Modifier
        .size(56.dp)
        .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
      ) {
        Icon(
          Icons.Default.Add,
          contentDescription = "Add shortcut",
          tint = iconTint,
          modifier = Modifier.size(24.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "Add",
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      color = labelColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      style = androidx.compose.ui.text.TextStyle(shadow = textShadow),
      modifier = Modifier.fillMaxWidth()
    )
  }
}

