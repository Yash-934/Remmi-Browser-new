--- app/src/main/java/com/remmi/browser/engine/GeckoPreferenceController.kt
+++ app/src/main/java/com/remmi/browser/engine/GeckoPreferenceController.kt
@@ -4,6 +4,7 @@
 import org.mozilla.geckoview.GeckoRuntime
 import org.mozilla.geckoview.GeckoPreferenceController as NativePrefCtrl
 import org.mozilla.geckoview.GeckoPreferenceController.SetGeckoPreference
+import kotlin.coroutines.resume
 
 class GeckoPreferenceController(private val runtime: GeckoRuntime?) {
   companion object {
