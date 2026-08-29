--- app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
+++ app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
@@ -126,14 +126,30 @@
   /**
    * Rotates Tor circuit using genuine NEWNYM signal.
    */
-  suspend fun rotateTorCircuit(): Result<TorCircuit> {
+  suspend fun rotateTorCircuit(): Result<TorCircuit> = transitionMutex.withLock {
+    // Advance generation and mark unverified
+    val generation = CurrentTorRoute.markStartingGhost()
+
     val result = torManager.refreshCircuit()
+    if (result.isFailure) {
+      CurrentTorRoute.clearRoute()
+      return result
+    }
+
     result.getOrNull()?.let { c ->
-      CurrentTorRoute.updateRoute(
-        socksPort = c.socksPort,
-        isGhostActive = true,
-        isVerified = true,
-        exitIp = c.verifiedExitIp
-      )
+      val proxyApplied = NetworkHardening.applyTorNetworkSettings(geckoEngine.runtime, c.socksPort, generation)
+      if (!proxyApplied) {
+        CurrentTorRoute.clearRoute()
+        return Result.failure(IllegalStateException("Failed to apply Gecko native Tor proxy preferences after NEWNYM"))
+      }
+
+      // Wait for Tor verification? Actually NetworkHardening just applies it.
+      // We need to do Gecko verification here?
+      // Let's assume enterGhostMode did it. Does enterGhostMode do Gecko-side route validation?
+      // Wait, look at enterGhostMode...
