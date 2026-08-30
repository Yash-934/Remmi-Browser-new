import re
path = "app/src/main/java/com/remmi/browser/MainActivity.kt"
with open(path, "r") as f:
    content = f.read()

target = """             if (err is IllegalStateException && err.message?.contains("RECOVERY REQUIRED") == true) {
                 com.remmi.browser.security.PanicWipeManager.markWipeInProgress(this@MainActivity, wipeVault = true)
                 currentScreen = ScreenRoute.EMERGENCY_RECOVERY
             }"""
replacement = """             if (err is com.remmi.browser.storage.VaultRecoveryRequiredException) {
                 currentScreen = ScreenRoute.VAULT_RECOVERY
             }"""
content = content.replace(target, replacement)
with open(path, "w") as f:
    f.write(content)
