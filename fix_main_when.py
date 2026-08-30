import re
path = "app/src/main/java/com/remmi/browser/MainActivity.kt"
with open(path, "r") as f:
    content = f.read()

target = """            ScreenRoute.EMERGENCY_RECOVERY -> {
              com.remmi.browser.ui.screens.EmergencyWipeRecoveryScreen(
                onRecoveryComplete = {
                  // After successful verification, initialize runtime and proceed to Browser
                  com.remmi.browser.engine.GeckoEngineManager.getInstance(applicationContext).initializeRuntimeAsync()
                  currentScreen = ScreenRoute.BROWSER
                }
              )
            }"""
replacement = """            ScreenRoute.EMERGENCY_RECOVERY -> {
              com.remmi.browser.ui.screens.EmergencyWipeRecoveryScreen(
                onRecoveryComplete = {
                  com.remmi.browser.engine.GeckoEngineManager.getInstance(applicationContext).initializeRuntimeAsync()
                  currentScreen = ScreenRoute.BROWSER
                }
              )
            }
            ScreenRoute.VAULT_RECOVERY -> {
              com.remmi.browser.ui.screens.VaultRecoveryScreen(
                onProceedToWipe = {
                  com.remmi.browser.security.PanicWipeManager.markWipeInProgress(this@MainActivity, wipeVault = true)
                  currentScreen = ScreenRoute.EMERGENCY_RECOVERY
                }
              )
            }"""
content = content.replace(target, replacement)
with open(path, "w") as f:
    f.write(content)
