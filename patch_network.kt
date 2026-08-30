--- app/src/main/java/com/remmi/browser/security/NetworkHardening.kt
+++ app/src/main/java/com/remmi/browser/security/NetworkHardening.kt
@@ -19,10 +19,10 @@
 object NetworkHardening {
 
   private const val TAG = "NetworkHardening"
 
   data class RouteKey(val profile: PrivacyProfile, val port: Int, val generation: Long, val runtimeHash: Int)
 
   @Volatile
   private var lastAppliedRouteKey: RouteKey? = null
 
   fun getTorPreferences(
@@ -58,25 +58,6 @@
       "network.http.referer.trimmingPolicy" to (if (settings?.strictReferrerPolicy != false) 2 else 0),
       "network.http.referer.XOriginPolicy" to (if (settings?.strictReferrerPolicy != false) 2 else 0),
       "network.http.referer.XOriginTrimmingPolicy" to (if (settings?.strictReferrerPolicy != false) 2 else 0),
-      // Smooth Scrolling & Hardware Acceleration
-      "general.smoothScroll" to true,
-      "general.smoothScroll.lines" to true,
-      "general.smoothScroll.pages" to true,
-      "general.smoothScroll.scrollbars" to true,
-      "general.smoothScroll.other" to true,
-      "general.smoothScroll.msdPhysics.enabled" to true,
-      "apz.overscroll.enabled" to true,
-      "apz.allow_zooming" to true,
-      "apz.touch_start_tolerance" to "0.05",
-      "apz.velocity_relevance_time_ms" to 300,
-      "apz.max_velocity_inches_per_ms" to "70.0",
-      "apz.fling_friction" to "0.002",
-      "layers.acceleration.force-enabled" to true,
-      "layers.async-pan-zoom.enabled" to true,
-      "layers.offmainthreadcomposition.enabled" to true,
-      "gfx.webrender.all" to true,
-      "gfx.webrender.compositor" to true,
-      "layout.css.touch_action.enabled" to true,
-      "layout.css.scroll-behavior.enabled" to true,
       "privacy.resistFingerprinting.reduceTimerPrecision.microseconds" to 16666,
     )
   }
@@ -124,24 +105,6 @@
       "network.http.referer.trimmingPolicy" to (if (settings?.strictReferrerPolicy != false) 2 else 0),
       "network.http.referer.XOriginPolicy" to (if (settings?.strictReferrerPolicy != false) 2 else 0),
       "network.http.referer.XOriginTrimmingPolicy" to (if (settings?.strictReferrerPolicy != false) 2 else 0),
-      // Smooth Scrolling & Hardware Acceleration
-      "general.smoothScroll" to true,
-      "general.smoothScroll.lines" to true,
-      "general.smoothScroll.pages" to true,
-      "general.smoothScroll.scrollbars" to true,
-      "general.smoothScroll.other" to true,
-      "general.smoothScroll.msdPhysics.enabled" to true,
-      "apz.overscroll.enabled" to true,
-      "apz.allow_zooming" to true,
-      "apz.touch_start_tolerance" to "0.05",
-      "apz.velocity_relevance_time_ms" to 300,
-      "apz.max_velocity_inches_per_ms" to "70.0",
-      "apz.fling_friction" to "0.002",
-      "layers.acceleration.force-enabled" to true,
-      "layers.async-pan-zoom.enabled" to true,
-      "layers.offmainthreadcomposition.enabled" to true,
-      "gfx.webrender.all" to true,
-      "gfx.webrender.compositor" to true,
-      "layout.css.touch_action.enabled" to true,
-      "layout.css.scroll-behavior.enabled" to true,
     )
   }
@@ -165,6 +128,15 @@
     val prefController = GeckoPreferenceController(runtime)
     val applied = prefController.applyPreferences(prefs, GeckoPreferenceController.PREF_BRANCH_USER)
     
+    // Get non-routing preferences and apply them without affecting routing result
+    val performancePrefs = getPerformancePreferences()
+    try {
+       prefController.applyPreferences(performancePrefs, GeckoPreferenceController.PREF_BRANCH_USER)
+    } catch(e: Exception) {
+       Log.w(TAG, "Failed to apply performance prefs", e)
+    }
+    
     if (applied) {
       val verifyKeys = listOf(
         "network.proxy.type",
@@ -201,6 +173,14 @@
     val prefController = GeckoPreferenceController(runtime)
     val applied = prefController.applyPreferences(prefs, GeckoPreferenceController.PREF_BRANCH_USER)
     
+    // Get non-routing preferences and apply them without affecting routing result
+    val performancePrefs = getPerformancePreferences()
+    try {
+       prefController.applyPreferences(performancePrefs, GeckoPreferenceController.PREF_BRANCH_USER)
+    } catch(e: Exception) {
+       Log.w(TAG, "Failed to apply performance prefs", e)
+    }
+
     if (applied) {
+      val verifyKeys = listOf(
+        "network.proxy.type",
+        "network.proxy.failover_direct"
+      )
+      val readBack = prefController.getPreferences(verifyKeys)
+      val isReadbackValid = readBack["network.proxy.type"] == 0 &&
+        readBack["network.proxy.failover_direct"] == true
+            
+      if (!isReadbackValid) {
+        Log.e(TAG, "Critical Shield preferences readback failed! Expected direct proxy but got: $readBack")
+        DebugLogManager.log("[ROUTE] gecko_proxy_failed profile=SHIELD reason=readback_mismatch")
+        return false
+      }
+      
       lastAppliedRouteKey = targetKey
       DebugLogManager.log("[ROUTE] NATIVE_GECKO_APPLIED profile=SHIELD")
@@ -216,6 +196,28 @@
   fun resetAppliedState() {
     lastAppliedRouteKey = null
   }
+  
+  fun getPerformancePreferences(): Map<String, Any> {
+    return mapOf(
+      "general.smoothScroll" to true,
+      "general.smoothScroll.lines" to true,
+      "general.smoothScroll.pages" to true,
+      "general.smoothScroll.scrollbars" to true,
+      "general.smoothScroll.other" to true,
+      "general.smoothScroll.msdPhysics.enabled" to true,
+      "apz.overscroll.enabled" to true,
+      "apz.allow_zooming" to true,
+      "apz.touch_start_tolerance" to "0.05", // Assuming string works, GeckoPreferenceController blocks floats natively
+      "apz.velocity_relevance_time_ms" to 300,
+      "apz.max_velocity_inches_per_ms" to "70.0",
+      "apz.fling_friction" to "0.002",
+      "layers.acceleration.force-enabled" to true,
+      "layers.async-pan-zoom.enabled" to true,
+      "layers.offmainthreadcomposition.enabled" to true,
+      "gfx.webrender.all" to true,
+      "gfx.webrender.compositor" to true,
+      "layout.css.touch_action.enabled" to true,
+      "layout.css.scroll-behavior.enabled" to true,
+    )
+  }
 
