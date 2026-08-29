--- app/src/main/java/com/remmi/browser/reader/ReaderTranslator.kt
+++ app/src/main/java/com/remmi/browser/reader/ReaderTranslator.kt
@@ -58,42 +58,28 @@
     try {
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
-
-          val body = resp.body?.string() ?: return@withTimeout text
+        val response = executor.fetch(request).poll(10000)
+        if (response == null || response.statusCode != 200) return@withTimeout text
+        
+        val bodyStream = response.body ?: return@withTimeout text
+        val body = Scanner(bodyStream, "UTF-8").useDelimiter("\\A").next()
+        bodyStream.close()
+        
           val jsonArray = JSONArray(body)
           val sentencesArray = jsonArray.optJSONArray(0) ?: return@withTimeout text
 
           val sb = StringBuilder()
           for (i in 0 until sentencesArray.length()) {
             val sentenceObj = sentencesArray.optJSONArray(i)
             if (sentenceObj != null && sentenceObj.length() > 0) {
               sb.append(sentenceObj.optString(0))
             }
           }
           val result = sb.toString().trim()
           if (result.isNotBlank()) result else text
-        }
       }
     } catch (e: Exception) {
@@ -109,6 +95,7 @@
    */
   suspend fun translateArticle(
+    context: Context,
     article: ReaderArticle,
     targetLanguageCode: String,
     isGhost: Boolean = false,
     onProgress: (Int, Int) -> Unit = { _, _ -> }
@@ -122,7 +109,7 @@
     val lang = SUPPORTED_LANGUAGES.firstOrNull { it.code == targetLanguageCode }?.displayName ?: targetLanguageCode
 
-    val translatedTitle = translateText(article.title, targetLanguageCode, isGhost)
+    val translatedTitle = translateText(context, article.title, targetLanguageCode, isGhost)
     val translatedParas = mutableListOf<ReaderParagraph>()
 
     val boundedParagraphs = article.paragraphs.take(200)
     val total = boundedParagraphs.size
     for ((idx, p) in boundedParagraphs.withIndex()) {
-      val translatedP = translateText(p.text, targetLanguageCode, isGhost)
+      val translatedP = translateText(context, p.text, targetLanguageCode, isGhost)
       translatedParas.add(p.copy(text = translatedP))
       onProgress(idx + 1, total)
