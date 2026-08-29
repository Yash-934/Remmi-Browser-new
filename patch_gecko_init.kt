--- app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
@@ -115,18 +115,18 @@
     _initState.value = GeckoInitState.INITIALIZING
     Log.i(TAG, "STATE_LOG: GECKO_INIT_START (time=${android.os.SystemClock.elapsedRealtime()})")
 
-    mainHandler.post {
+    CoroutineScope(Dispatchers.Main.immediate).launch {
       try {
         initializeRuntimeInternal()
         _initState.value = GeckoInitState.READY
         Log.i(TAG, "STATE_LOG: GECKO_INIT_READY (time=${android.os.SystemClock.elapsedRealtime()})")
       } catch (t: Throwable) {
         _initState.value = GeckoInitState.FAILED
         Log.e(TAG, "STATE_LOG: GECKO_INIT_FAILED (time=${android.os.SystemClock.elapsedRealtime()}) error=${t.message}")
       }
     }
   }
 
-  private fun initializeRuntimeInternal() {
+  private suspend fun initializeRuntimeInternal() {
     assertMainThread("INITIALIZE_RUNTIME_INTERNAL")
     if (runtime != null) {
       _initState.value = GeckoInitState.READY
@@ -192,20 +192,20 @@
-      rt.webExtensionController
-        .ensureBuiltIn(extensionUri, "extension@remmi.browser")
-        .accept(
-          { ext: WebExtension? -> installPromptHandler(ext) },
-          { throwable: Throwable? -> failureHandler(throwable) }
-        )
+      kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
+        rt.webExtensionController
+          .ensureBuiltIn(extensionUri, "extension@remmi.browser")
+          .accept(
+            { ext: WebExtension? -> installPromptHandler(ext); cont.resume(Unit) },
+            { throwable: Throwable? -> failureHandler(throwable); cont.resume(Unit) }
+          )
+      }
     } catch (e: Exception) {
       Log.w(TAG, "WebExtension installation skipped: ${e.message}")
       blockExtension.setExtensionFailed(e.message ?: "Skipped")
       com.remmi.browser.util.DebugLogManager.log("[WEBEXT] Installation exception: ${e.message}")
     }
 
     runtime = rt
-    _initState.value = GeckoInitState.READY
     applyPrivacyProfile(PrivacyProfile.SHIELD)
     val duration = android.os.SystemClock.elapsedRealtime() - startTime
