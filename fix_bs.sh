sed -i '/LaunchedEffect(settings.defaultProfile) {/,+2d' app/src/main/java/com/remmi/browser/ui/screens/BrowserScreen.kt
sed -i '/val settings by settingsRepo.settings.collectAsState()/a \  LaunchedEffect(settings.defaultProfile) {\n    tabManager.updateInitialTabProfile(settings.defaultProfile)\n  }' app/src/main/java/com/remmi/browser/ui/screens/BrowserScreen.kt
