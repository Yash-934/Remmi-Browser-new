package com.remmi.browser.ui.components

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.remmi.browser.downloads.DownloadHandler
import com.remmi.browser.engine.BrowserTab
import com.remmi.browser.engine.GeckoEngineManager
import com.remmi.browser.engine.GeckoTabCallbacks
import com.remmi.browser.model.WebContextMenuData
import com.remmi.browser.reader.ReaderArticle
import com.remmi.browser.reader.ReaderExtractor
import com.remmi.browser.security.CurrentTorRoute
import com.remmi.browser.security.NetworkHardening
import com.remmi.browser.security.PrivacyProfile
import com.remmi.browser.ui.theme.ThemeCyber
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebResponse

@Composable
fun BrowserView(
  tab: BrowserTab,
  onUrlChange: (String) -> Unit,
  onTitleChange: (String) -> Unit,
  onProgressChange: (Int) -> Unit,
  onLoadingChange: (Boolean) -> Unit,
  onSecurityChange: (Boolean) -> Unit,
  onNavStateChange: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
  onTrackerBlocked: (url: String, type: String) -> Unit,
  onReaderArticleExtracted: ((ReaderArticle) -> Unit)? = null,
  onContextMenuRequested: ((WebContextMenuData) -> Unit)? = null,
  onScrollChange: ((isScrollingDown: Boolean) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val scope = rememberCoroutineScope()
  val geckoEngine = remember { GeckoEngineManager.getInstance(context) }
  val downloadHandler = remember { DownloadHandler.getInstance(context) }

  var geckoViewRef by remember { mutableStateOf<GeckoView?>(null) }
  var progressFloat by remember { mutableFloatStateOf(0f) }
  val animatedProgress by animateFloatAsState(
    targetValue = progressFloat,
    label = "cyber_progress",
  )

  // Callbacks bundle decoupled from GeckoSession
  val tabCallbacks = remember(tab.id, tab.profile) {
    object : GeckoTabCallbacks {
      override fun onUrlChange(url: String) {
        onUrlChange(url)
      }

      override fun onTitleChange(title: String) {
        onTitleChange(title)
      }

      override fun onProgressChange(progress: Int) {
        progressFloat = (progress.toFloat() / 100f).coerceIn(0f, 1f)
        onProgressChange(progress)
      }

      override fun onLoadingChange(isLoading: Boolean) {
        onLoadingChange(isLoading)
        if (isLoading) {
          progressFloat = 0.1f
        } else {
          progressFloat = 0f
        }
      }

      override fun onSecurityChange(isSecure: Boolean) {
        onSecurityChange(isSecure)
      }

      override fun onNavStateChange(canGoBack: Boolean, canGoForward: Boolean) {
        onNavStateChange(canGoBack, canGoForward)
      }

      override fun onTrackerBlocked(url: String, type: String) {
        onTrackerBlocked(url, type)
      }

      override fun onScrollChanged(scrollX: Int, scrollY: Int, isScrollingDown: Boolean) {
        onScrollChange?.invoke(isScrollingDown)
      }

      override fun onExternalResponse(response: WebResponse) {
        val uri = response.uri
        val contentDisposition = response.headers["Content-Disposition"]
        val filename = contentDisposition?.substringAfter("filename=")?.trim('"', '\'', ' ')
          ?: uri.substringAfterLast('/').substringBefore('?').ifEmpty { "download_${System.currentTimeMillis()}" }
        val contentType = response.headers["Content-Type"] ?: "application/octet-stream"
        val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: 0L

        downloadHandler.enqueueDownload(
          url = uri,
          suggestedFilename = filename,
          mimeType = contentType,
          contentLength = contentLength,
          isGhost = tab.profile == PrivacyProfile.GHOST,
          webResponse = response
        )
      }

      override fun onContextMenu(data: WebContextMenuData) {
        onContextMenuRequested?.invoke(data)
      }
    }
  }

  // Explicit lifecycle observer to re-activate tab session safely on app resume/pause
  DisposableEffect(lifecycleOwner, tab.id) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          scope.launch {
            geckoEngine.setTabActive(tab.id, true)
            geckoViewRef?.let { gv ->
              gv.visibility = View.VISIBLE
              geckoEngine.attachView(
                tabId = tab.id,
                geckoView = gv,
                profile = tab.profile,
                isDesktopMode = tab.isDesktopMode,
                securityLevel = tab.securityLevel,
                containerType = tab.containerType,
                callbacks = tabCallbacks,
              )
              gv.requestLayout()
              gv.invalidate()
            }
          }
        }
        Lifecycle.Event.ON_PAUSE -> {
          scope.launch {
            geckoEngine.setTabActive(tab.id, false)
          }
        }
        else -> {}
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Synchronize network hardening and privacy profile settings
  LaunchedEffect(tab.id, tab.profile, tab.isDesktopMode, tab.securityLevel, tab.containerType) {
    if (tab.profile == PrivacyProfile.GHOST) {
      val port = CurrentTorRoute.currentSocksPort
      NetworkHardening.applyTorNetworkSettings(geckoEngine.runtime, port)
      geckoEngine.applyPrivacyProfile(PrivacyProfile.GHOST, tab.securityLevel, port)
    } else {
      NetworkHardening.applyShieldNetworkSettings(geckoEngine.runtime)
      geckoEngine.applyPrivacyProfile(PrivacyProfile.SHIELD, tab.securityLevel)
    }
    geckoEngine.updateTabSettings(tab.id, tab.isDesktopMode, tab.profile, tab.securityLevel)
  }

  // Handle URL navigation safely through GeckoEngineManager
  LaunchedEffect(tab.url) {
    if (tab.url.isNotBlank() && tab.url != "about:blank" && tab.url != "netrunner://newtab") {
      if (tab.profile == PrivacyProfile.GHOST) {
        val port = CurrentTorRoute.currentSocksPort
        NetworkHardening.applyTorNetworkSettings(geckoEngine.runtime, port)
      }
      geckoEngine.loadUrl(tab.id, tab.url)
    }
  }

  // Handle Reader Mode trigger with real web content extraction
  LaunchedEffect(tab.isReaderMode, tab.url) {
    if (tab.isReaderMode && tab.readerArticle == null && tab.url.isNotBlank() && tab.url != "about:blank") {
      try {
        val blockExtension = geckoEngine.blockExtension
        val htmlDeferred = CompletableDeferred<String>()
        val originalCallback = blockExtension.onHtmlExtracted
        blockExtension.onHtmlExtracted = { _, html ->
          htmlDeferred.complete(html)
        }
        blockExtension.extractActiveTabHtml()

        val pageHtml = withTimeoutOrNull(2500L) { htmlDeferred.await() }
        blockExtension.onHtmlExtracted = originalCallback

        val extracted = if (!pageHtml.isNullOrBlank()) {
          val domain = try {
            java.net.URI(tab.url).host ?: tab.url.substringAfter("://").substringBefore('/')
          } catch (e: Exception) {
            tab.url.substringAfter("://").substringBefore('/')
          }
          ReaderExtractor.parseHtmlDocument(pageHtml, tab.url, tab.title, domain)
        } else {
          val isGhost = tab.profile == PrivacyProfile.GHOST
          ReaderExtractor.extractFromUrl(tab.url, tab.title, isGhost)
        }

        onReaderArticleExtracted?.invoke(extracted)
      } catch (e: Exception) {
        val domain = tab.url.substringAfter("://").substringBefore('/')
        val fallback = ReaderArticle(
          title = tab.title.ifEmpty { "Article on $domain" },
          siteName = domain,
          paragraphs = listOf(
            com.remmi.browser.reader.ReaderParagraph(0, "Extracted content from ${tab.title.ifEmpty { tab.url }}."),
            com.remmi.browser.reader.ReaderParagraph(1, "Original page source: ${tab.url}")
          ),
          rawTextList = listOf("Extracted content from ${tab.title.ifEmpty { tab.url }}."),
          readingTimeMinutes = 1,
          sourceUrl = tab.url,
        )
        onReaderArticleExtracted?.invoke(fallback)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ThemeCyber.colors.background)
  ) {
    val isNewTab = tab.url.isBlank() || tab.url == "about:blank" || tab.url == "netrunner://newtab"

    if (isNewTab) {
      NewTabPage(
        profile = tab.profile,
        blockedTrackersCount = tab.blockedTrackersCount,
        onNavigate = { target ->
          onUrlChange(target)
        },
        modifier = Modifier.fillMaxSize()
      )
    } else {
      AndroidView(
        factory = { ctx ->
          GeckoView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE
            geckoViewRef = this

            var startTouchY = 0f
            setOnTouchListener { _, event ->
              when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                  startTouchY = event.rawY
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                  val deltaY = event.rawY - startTouchY
                  if (deltaY < -24f) {
                    // Finger dragged up -> scrolling down page -> hide URL bar
                    onScrollChange?.invoke(true)
                    startTouchY = event.rawY
                  } else if (deltaY > 24f) {
                    // Finger dragged down -> scrolling up page -> reveal URL bar
                    onScrollChange?.invoke(false)
                    startTouchY = event.rawY
                  }
                }
              }
              false
            }

            scope.launch {
              geckoEngine.attachView(
                tabId = tab.id,
                geckoView = this@apply,
                profile = tab.profile,
                isDesktopMode = tab.isDesktopMode,
                securityLevel = tab.securityLevel,
                containerType = tab.containerType,
                callbacks = tabCallbacks,
              )
              if (tab.url.isNotBlank() && tab.url != "about:blank" && tab.url != "netrunner://newtab") {
                geckoEngine.loadUrl(tab.id, tab.url)
              }
            }
          }
        },
        update = { geckoView ->
          geckoViewRef = geckoView
          geckoView.visibility = View.VISIBLE
          scope.launch {
            geckoEngine.attachView(
              tabId = tab.id,
              geckoView = geckoView,
              profile = tab.profile,
              isDesktopMode = tab.isDesktopMode,
              securityLevel = tab.securityLevel,
              containerType = tab.containerType,
              callbacks = tabCallbacks,
            )
          }
        },
        onRelease = { geckoView ->
          geckoViewRef = null
          scope.launch {
            geckoEngine.detachView(tab.id, geckoView)
          }
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("gecko_browser_view"),
      )
    }

    // Top Glowing Cyber Progress Bar
    if (tab.isLoading && animatedProgress > 0f) {
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .align(Alignment.TopCenter)
          .testTag("cyber_loading_progress_bar"),
        color = if (tab.profile == PrivacyProfile.GHOST) ThemeCyber.colors.torPurple else ThemeCyber.colors.primary,
        trackColor = ThemeCyber.colors.surfaceLight,
      )
    }
  }
}
