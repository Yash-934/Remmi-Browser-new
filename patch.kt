--- app/src/main/java/com/remmi/browser/security/NetworkHardening.kt
+++ app/src/main/java/com/remmi/browser/security/NetworkHardening.kt
@@ -165,7 +165,7 @@
       return false
     }
-    val targetKey = RouteKey(PrivacyProfile.GHOST, port, generation)
+    val targetKey = RouteKey(PrivacyProfile.GHOST, port, generation, System.identityHashCode(runtime))
     if (lastAppliedRouteKey == targetKey) {
       // Idempotent: configuration already active
       return true
     }
@@ -176,7 +176,7 @@
       lastAppliedRouteKey = targetKey
       DebugLogManager.log("[ROUTE] NATIVE_GECKO_APPLIED profile=GHOST port=$port")
     } else {
-      DebugLogManager.log("[ROUTE] NATIVE_GECKO_DISPATCHED profile=GHOST port=$port")
+      DebugLogManager.log("[ROUTE] gecko_proxy_failed profile=GHOST port=$port")
     }
     return applied
   }
@@ -204,7 +204,7 @@
       lastAppliedRouteKey = targetKey
       DebugLogManager.log("[ROUTE] NATIVE_GECKO_APPLIED profile=SHIELD")
     } else {
-      DebugLogManager.log("[ROUTE] NATIVE_GECKO_DISPATCHED profile=SHIELD")
+      DebugLogManager.log("[ROUTE] gecko_proxy_failed profile=SHIELD")
     }
     return applied
   }
