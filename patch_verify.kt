--- app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
+++ app/src/main/java/com/remmi/browser/security/PrivacyNetworkController.kt
@@ -10,6 +10,12 @@
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.sync.Mutex
 import kotlinx.coroutines.sync.withLock
 import kotlinx.coroutines.withContext
+import org.mozilla.geckoview.GeckoWebExecutor
+import org.mozilla.geckoview.WebRequest
+import java.io.InputStream
+import java.util.Scanner
 
 /**
  * Single, authoritative controller for all Tor/Ghost network transactions.
@@ -81,6 +87,27 @@
     }
 
+    // Step 5: Verify Gecko is actually using the expected route
+    val geckoVerified = try {
+      val executor = GeckoWebExecutor(geckoEngine.runtime!!)
+      val request = WebRequest.Builder("https://check.torproject.org/api/ip").build()
+      val response = executor.fetch(request).poll(10000)
+      if (response != null && response.statusCode == 200) {
+        val bodyStream = response.body
+        if (bodyStream != null) {
+          val content = Scanner(bodyStream, "UTF-8").useDelimiter("\\A").next()
+          bodyStream.close()
+          content.contains("\"IsTor\":true")
+        } else false
+      } else false
+    } catch (e: Exception) {
+      false
+    }
+    if (!geckoVerified) {
+      CurrentTorRoute.clearRoute()
+      return@withContext Result.failure(IllegalStateException("Gecko native Tor proxy verification failed"))
+    }
+
-    // Step 5: Advance route generation and update Single Source of Truth
+    // Step 6: Advance route generation and update Single Source of Truth
     CurrentTorRoute.updateRoute(
       socksPort = socksPort,
