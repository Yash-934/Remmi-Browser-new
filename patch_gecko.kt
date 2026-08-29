--- app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoEngineManager.kt
@@ -635,6 +635,10 @@
     android.util.Log.i(TAG, "STATE_LOG: FIRST_PAGE_START (time=${android.os.SystemClock.elapsedRealtime()})")
+    if (_initState.value == GeckoInitState.NOT_STARTED) {
+      Log.d(TAG, "[GECKO] loadUrl requesting init on tabId=$tabId")
+      initializeRuntimeAsync()
+    }
     if (runtime == null || _initState.value != GeckoInitState.READY) {
       Log.d(TAG, "[GECKO] loadUrl runtime not ready yet, deferring 50ms for tabId=$tabId")
