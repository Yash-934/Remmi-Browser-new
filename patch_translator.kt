--- app/src/main/java/com/remmi/browser/reader/ReaderTranslator.kt
+++ app/src/main/java/com/remmi/browser/reader/ReaderTranslator.kt
@@ -1,5 +1,6 @@
 package com.remmi.browser.reader
 
+import android.content.Context
 import android.util.Log
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.withContext
-import okhttp3.OkHttpClient
-import okhttp3.Request
+import org.mozilla.geckoview.GeckoWebExecutor
+import org.mozilla.geckoview.WebRequest
 import org.json.JSONArray
 import java.net.URLEncoder
+import java.util.Scanner
 
@@ -45,14 +46,5 @@
 
-  private fun getClient(isGhost: Boolean): OkHttpClient {
-    return com.remmi.browser.security.NetworkRouteAuthority.createHttpClient(
-      isGhost = isGhost,
-      connectTimeoutSeconds = 8L,
-      readTimeoutSeconds = 10L,
-      followRedirects = true
-    )
-  }
-
   /**
    * Translates a single text string to target language
    */
-  suspend fun translateText(text: String, targetLanguageCode: String, isGhost: Boolean = false): String = withContext(Dispatchers.IO) {
+  suspend fun translateText(context: Context, text: String, targetLanguageCode: String, isGhost: Boolean = false): String = withContext(Dispatchers.IO) {
     if (text.isBlank()) return@withContext ""
@@ -67,31 +59,27 @@
       kotlinx.coroutines.withTimeout(10_000L) {
-        val client = try {
-          getClient(isGhost)
-        } catch (e: Exception) {
-          Log.w(TAG, "Ghost client creation failed: ${e.message}")
-          return@withTimeout text
+        val runtime = com.remmi.browser.engine.GeckoEngineManager.getInstance(context).runtime
+        if (runtime == null) {
+          Log.w(TAG, "Ghost client creation failed: Gecko runtime not available")
+          return@withTimeout text
         }
 
         val encoded = URLEncoder.encode(boundedText, "UTF-8")
         val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLanguageCode&dt=t&q=$encoded"
 
-        val request = Request.Builder()
-          .url(url)
-          .header("User-Agent", "Mozilla/5.0")
+        val executor = GeckoWebExecutor(runtime)
+        val request = WebRequest.Builder(url)
+          .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
           .build()
 
-        val call = client.newCall(request)
-        val response = try {
-          call.execute()
-        } catch (e: Exception) {
-          call.cancel()
-          throw e
-        }
-
-        response.use { resp ->
-          if (!resp.isSuccessful) return@withTimeout text
-          val responseBody = resp.body?.string() ?: return@withTimeout text
+        val response = executor.fetch(request).poll(10000)
+        if (response == null || response.statusCode != 200) return@withTimeout text
+        
+        val bodyStream = response.body ?: return@withTimeout text
+        val responseBody = Scanner(bodyStream, "UTF-8").useDelimiter("\\A").next()
+        bodyStream.close()
 
           val jsonArray = JSONArray(responseBody)
           val result = StringBuilder()
@@ -118,2 +106,3 @@
   suspend fun translateArticle(
+    context: Context,
     article: ReaderArticle,
@@ -128,11 +117,11 @@
     }
 
-    val translatedTitle = translateText(article.title, targetLanguageCode, isGhost)
+    val translatedTitle = translateText(context, article.title, targetLanguageCode, isGhost)
     val translatedParagraphs = mutableListOf<ReaderParagraph>()
 
     for (p in article.paragraphs) {
-      val translatedText = translateText(p.text, targetLanguageCode, isGhost)
+      val translatedText = translateText(context, p.text, targetLanguageCode, isGhost)
       translatedParagraphs.add(p.copy(text = translatedText))
