--- app/src/main/java/com/remmi/browser/reader/ReaderModel.kt
+++ app/src/main/java/com/remmi/browser/reader/ReaderModel.kt
@@ -10,13 +10,14 @@
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.withContext
-import okhttp3.OkHttpClient
-import okhttp3.Request
 import org.json.JSONObject
 import org.jsoup.Jsoup
 import org.jsoup.nodes.Document
 import org.jsoup.nodes.Element
+import org.mozilla.geckoview.GeckoWebExecutor
+import org.mozilla.geckoview.WebRequest
 import java.net.InetSocketAddress
 import java.net.Proxy
 import java.net.URI
+import java.util.Scanner
 import java.util.concurrent.TimeUnit
 
@@ -126,16 +127,7 @@
   private const val MAX_RESPONSE_BYTES = 2L * 1024L * 1024L // 2 MB cap
   private const val MAX_ARTICLE_PARAGRAPHS = 500
 
-  private fun getClient(isGhost: Boolean, url: String? = null): OkHttpClient {
-    return com.remmi.browser.security.NetworkRouteAuthority.createHttpClient(
-      isGhost = isGhost,
-      targetUrl = url,
-      connectTimeoutSeconds = 10L,
-      readTimeoutSeconds = 15L,
-      followRedirects = true
-    )
-  }
-
   /**
    * Fetches the web page asynchronously and extracts full clean article content
    */
-  suspend fun extractFromUrl(url: String, currentTitle: String = "", isGhost: Boolean = false): ReaderArticle = withContext(Dispatchers.IO) {
+  suspend fun extractFromUrl(context: Context, url: String, currentTitle: String = "", isGhost: Boolean = false): ReaderArticle = withContext(Dispatchers.IO) {
     val domain = try {
       URI(url).host ?: url.substringAfter("://").substringBefore('/')
@@ -160,42 +152,36 @@
       return@withContext createFallbackArticle(url, currentTitle, domain, "Extraction blocked: Tor route is not verified")
     }
 
-    val client = try {
-      getClient(isGhost || com.remmi.browser.security.NetworkRouteAuthority.isOnionDestination(url), url)
-    } catch (e: Exception) {
-      return@withContext createFallbackArticle(url, currentTitle, domain, "Network authority error: ${e.message}")
-    }
-
+    val runtime = com.remmi.browser.engine.GeckoEngineManager.getInstance(context).runtime
+    if (runtime == null) {
+      return@withContext createFallbackArticle(url, currentTitle, domain, "Gecko runtime not available")
+    }
+    
     try {
       kotlinx.coroutines.withTimeout(20_000L) {
-        val request = Request.Builder()
-          .url(url)
-          .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
-          .header("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.8")
+        val executor = GeckoWebExecutor(runtime)
+        val request = WebRequest.Builder(url)
+          .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
           .build()
 
-        val call = client.newCall(request)
-        // Coroutine cancellation support
-        val response = try {
-          call.execute()
-        } catch (e: Exception) {
-          call.cancel()
-          throw e
-        }
-
-        response.use { resp ->
-          if (!resp.isSuccessful) {
-            return@withTimeout createFallbackArticle(url, currentTitle, domain, "HTTP error ${resp.code}")
-          }
-
-          val contentType = resp.header("Content-Type", "")?.lowercase() ?: ""
-          if (contentType.isNotEmpty() && !contentType.contains("text/html") && !contentType.contains("xhtml") && !contentType.contains("text/plain")) {
-            return@withTimeout createFallbackArticle(url, currentTitle, domain, "Unsupported Content-Type: $contentType")
-          }
-
-          val responseBody = resp.body
-          if (responseBody == null) {
-            return@withTimeout createFallbackArticle(url, currentTitle, domain, "Empty page response")
-          }
-
-          val html = responseBody.string()
-
-          if (html.isBlank()) {
-            return@withTimeout createFallbackArticle(url, currentTitle, domain, "Empty page response")
-          }
-
-          parseHtmlDocument(html, url, currentTitle, domain)
-        }
+        val response = executor.fetch(request).poll(15000)
+        if (response == null) {
+          return@withTimeout createFallbackArticle(url, currentTitle, domain, "Request timed out")
+        }
+        if (response.statusCode != 200) {
+           return@withTimeout createFallbackArticle(url, currentTitle, domain, "HTTP error ${response.statusCode}")
+        }
+        
+        val bodyStream = response.body
+        if (bodyStream == null) {
+           return@withTimeout createFallbackArticle(url, currentTitle, domain, "Empty page response")
+        }
+        val html = Scanner(bodyStream, "UTF-8").useDelimiter("\\A").next()
+        bodyStream.close()
+        if (html.isBlank()) {
+           return@withTimeout createFallbackArticle(url, currentTitle, domain, "Empty page response")
+        }
+        parseHtmlDocument(html, url, currentTitle, domain)
       }
     } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
