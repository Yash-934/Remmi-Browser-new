--- app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
+++ app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
@@ -148,7 +148,7 @@
    * 2. Clears SOCKS proxy from WebExtension & native Gecko engine.
    * 3. Stops Tor and updates all tabs to reflect the direct clearnet routing.
    */
-  suspend fun enterShieldMode(tabId: String) = withContext(Dispatchers.IO) {
+  suspend fun enterShieldMode(tabId: String): Unit = transitionMutex.withLock { withContext(Dispatchers.IO) {
     Log.i(TAG, "Entering Shield Mode for tab $tabId (restoring direct clearnet)...")
     geckoEngine.closeSessionSafely(tabId)
     val generation = CurrentTorRoute.clearRoute()
@@ -163,6 +163,7 @@
     DebugLogManager.log("[ROUTE] ACTIVE profile=SHIELD")
+  }
   }
 
   /**
@@ -219,10 +220,7 @@
    * Checks if Ghost Mode is currently verified and ready.
    */
   fun isGhostRoutingReady(): Boolean {
-    val state = torManager.bootstrapState.value
-    return state is TorManager.TorState.READY &&
-      TorStatusChecker.isPortListening("127.0.0.1", state.port, 200) &&
-      CurrentTorRoute.isGhostActive
+    return CurrentTorRoute.isReady
   }
 
