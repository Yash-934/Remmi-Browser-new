--- app/src/main/java/com/remmi/browser/engine/GeckoPreferenceController.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoPreferenceController.kt
@@ -10,6 +10,7 @@
   companion object {
     const val PREF_BRANCH_USER: Int = NativePrefCtrl.PREF_BRANCH_USER
     const val PREF_BRANCH_DEFAULT: Int = NativePrefCtrl.PREF_BRANCH_DEFAULT
     private const val TAG = "GeckoPreferenceCtrl"
   }
 
+  suspend fun getPreferences(keys: List<String>): Map<String, Any?> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
+    try {
+      NativePrefCtrl.getGeckoPrefs(keys.toMutableList()).accept(
+        { result ->
+          if (cont.isActive) cont.resume(result ?: emptyMap())
+        },
+        {
+          if (cont.isActive) cont.resume(emptyMap())
+        }
+      )
+    } catch (t: Throwable) {
+      if (cont.isActive) cont.resume(emptyMap())
+    }
+  }
+
   suspend fun applyPreferences(prefs: Map<String, Any>, branch: Int = PREF_BRANCH_USER): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
