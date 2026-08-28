package com.remmi.browser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.remmi.browser.engine.TabManager
import com.remmi.browser.security.PanicWipeManager
import com.remmi.browser.storage.NetRunnerDatabase
import com.remmi.browser.storage.SettingsRepository
import com.remmi.browser.ui.passwords.PasswordManagerScreen
import com.remmi.browser.ui.screens.BrowserScreen
import com.remmi.browser.ui.screens.DebugLogsScreen
import com.remmi.browser.ui.screens.SettingsScreen
import com.remmi.browser.ui.screens.WelcomeScreen
import com.remmi.browser.ui.theme.RemmiTheme

import com.remmi.browser.ui.components.CrashReportDialog
import com.remmi.browser.util.CrashExportResult
import com.remmi.browser.util.CrashHandlerHelper

enum class ScreenRoute {
  WELCOME,
  BROWSER,
  SETTINGS,
  PASSWORDS,
  DEBUG_LOGS,
  EMERGENCY_RECOVERY
}

class MainActivity : FragmentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Check and export any previous crash report to Downloads/Remmi Browser/ and clipboard
    val pendingCrash = CrashHandlerHelper.checkAndExportPendingCrash(this)
    if (pendingCrash != null) {
      android.widget.Toast.makeText(
        this,
        "Crash log saved to Downloads/Remmi Browser/ & copied to clipboard",
        android.widget.Toast.LENGTH_LONG
      ).show()
    }

    enableEdgeToEdge()

    val isPendingWipe = PanicWipeManager.isWipePending(applicationContext)

    // IF no pending wipe, perform normal Gecko initialization immediately
    if (!isPendingWipe) {
      com.remmi.browser.engine.GeckoEngineManager.getInstance(applicationContext).initializeRuntime()
    }

    val prefs = getSharedPreferences("netrunner_app_state", Context.MODE_PRIVATE)
    val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)
    val settingsRepo = SettingsRepository.getInstance(applicationContext)

    val incomingUrl = handleIncomingIntent(intent)
    val initialScreen = when {
      isPendingWipe -> ScreenRoute.EMERGENCY_RECOVERY
      incomingUrl != null || hasSeenOnboarding -> ScreenRoute.BROWSER
      else -> ScreenRoute.WELCOME
    }

    setContent {
      val settings by settingsRepo.settings.collectAsState()
      var crashResultToShow by remember { mutableStateOf(pendingCrash) }

      RemmiTheme(
        cyberTheme = settings.cyberTheme,
        pureBlackOled = settings.pureBlackOled,
        browserFont = settings.browserFont,
      ) {
        var currentScreen by remember {
          mutableStateOf(initialScreen)
        }

        if (crashResultToShow != null) {
          CrashReportDialog(
            crashResult = crashResultToShow!!,
            onDismiss = { crashResultToShow = null }
          )
        }

        AnimatedContent(
          targetState = currentScreen,
          transitionSpec = { fadeIn() togetherWith fadeOut() },
          label = "screen_navigation",
        ) { screen ->
          when (screen) {
            ScreenRoute.EMERGENCY_RECOVERY -> {
              com.remmi.browser.ui.screens.EmergencyWipeRecoveryScreen(
                onRecoveryComplete = {
                  // After successful verification, initialize runtime and proceed to Browser
                  com.remmi.browser.engine.GeckoEngineManager.getInstance(applicationContext).initializeRuntime()
                  currentScreen = ScreenRoute.BROWSER
                }
              )
            }
            ScreenRoute.WELCOME -> {
              WelcomeScreen(
                onEnterBrowser = {
                  prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                  currentScreen = ScreenRoute.BROWSER
                }
              )
            }
            ScreenRoute.BROWSER -> {
              BrowserScreen(
                onOpenSettings = {
                  currentScreen = ScreenRoute.SETTINGS
                },
                onOpenWelcome = {
                  currentScreen = ScreenRoute.WELCOME
                },
                onOpenPasswords = {
                  currentScreen = ScreenRoute.PASSWORDS
                }
              )
            }
            ScreenRoute.SETTINGS -> {
              SettingsScreen(
                onBack = {
                  currentScreen = ScreenRoute.BROWSER
                },
                onOpenPasswords = {
                  currentScreen = ScreenRoute.PASSWORDS
                },
                onOpenDebugLogs = {
                  currentScreen = ScreenRoute.DEBUG_LOGS
                }
              )
            }
            ScreenRoute.PASSWORDS -> {
              PasswordManagerScreen(
                onBack = {
                  currentScreen = ScreenRoute.SETTINGS
                }
              )
            }
            ScreenRoute.DEBUG_LOGS -> {
              DebugLogsScreen(
                onBack = {
                  currentScreen = ScreenRoute.SETTINGS
                }
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?): String? {
    val data = intent?.dataString
    if (!data.isNullOrBlank() && (data.startsWith("http://") || data.startsWith("https://"))) {
      TabManager.getInstance().openOrNavigateTab(url = data)
      return data
    }
    return null
  }
}
