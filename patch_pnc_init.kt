--- app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
+++ app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
@@ -10,6 +10,8 @@
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.sync.Mutex
 import kotlinx.coroutines.sync.withLock
 import kotlinx.coroutines.withContext
+import kotlinx.coroutines.CoroutineScope
+import kotlinx.coroutines.launch
 import org.mozilla.geckoview.GeckoWebExecutor
 import org.mozilla.geckoview.WebRequest
@@ -29,6 +31,18 @@
   private val transitionMutex = Mutex()
 
+  init {
+    CoroutineScope(Dispatchers.Default).launch {
+      torManager.bootstrapState.collect { state ->
+        if (state is TorManager.TorState.OFF || state is TorManager.TorState.FAILED || state is TorManager.TorState.STOPPING) {
+          if (CurrentTorRoute.isGhostActive) {
+            Log.w(TAG, "Tor stopped unexpectedly while Ghost active. Invalidating route!")
+            CurrentTorRoute.clearRoute()
+          }
+        }
+      }
+    }
+  }
+
   /**
    * Enters Ghost Mode (Fail-Closed Tor Routing):
