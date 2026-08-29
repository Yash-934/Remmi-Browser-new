--- app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
@@ -218,22 +218,9 @@
     val rt = runtime ?: return
     val browserSettings = settings ?: com.remmi.browser.storage.SettingsRepository.getInstance(context).settings.value
 
-    if (profile == PrivacyProfile.GHOST) {
-      if (socksPort != null && socksPort > 0) {
-        // GHOST MODE (Native Gecko proxy routing + Tor SOCKS5 + Full RFP)
-        NetworkHardening.applyTorNetworkSettings(rt, socksPort, generation, browserSettings)
-      } else {
-        Log.w(TAG, "Cannot apply Ghost profile: SOCKS port is not ready ($socksPort)")
-      }
-    } else {
-      if (CurrentTorRoute.isGhostActive) {
-        Log.i(TAG, "Maintaining Tor routing invariant: Tor route active across system, skipping clearnet reset.")
-      } else {
-        // SHIELD / INCOGNITO MODE (Clearnet + FPP)
-        NetworkHardening.applyShieldNetworkSettings(rt, generation, browserSettings)
-      }
+    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
+      if (profile != PrivacyProfile.GHOST && !CurrentTorRoute.isGhostActive) {
+        NetworkHardening.applyShieldNetworkSettings(rt, generation, browserSettings)
+      }
     }
   }
