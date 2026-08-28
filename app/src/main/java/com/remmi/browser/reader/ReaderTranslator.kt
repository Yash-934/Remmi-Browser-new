package com.remmi.browser.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class TranslationLanguage(
  val code: String,
  val displayName: String,
  val nativeName: String,
)

object ReaderTranslator {
  private const val TAG = "ReaderTranslator"

  val SUPPORTED_LANGUAGES = listOf(
    TranslationLanguage("hi", "Hindi", "हिन्दी"),
    TranslationLanguage("es", "Spanish", "Español"),
    TranslationLanguage("fr", "French", "Français"),
    TranslationLanguage("de", "German", "Deutsch"),
    TranslationLanguage("ja", "Japanese", "日本語"),
    TranslationLanguage("zh-CN", "Chinese", "中文"),
    TranslationLanguage("ru", "Russian", "Русский"),
    TranslationLanguage("ar", "Arabic", "العربية"),
    TranslationLanguage("pt", "Portuguese", "Português"),
    TranslationLanguage("it", "Italian", "Italiano"),
    TranslationLanguage("bn", "Bengali", "বাংলা"),
    TranslationLanguage("mr", "Marathi", "मराठी"),
    TranslationLanguage("ta", "Tamil", "தமிழ்"),
    TranslationLanguage("te", "Telugu", "తెలుగు"),
    TranslationLanguage("gu", "Gujarati", "ગુજરાતી"),
    TranslationLanguage("ko", "Korean", "한국어"),
  )

  private fun getClient(isGhost: Boolean): OkHttpClient {
    val builder = OkHttpClient.Builder()
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
    if (isGhost) {
      val port = com.remmi.browser.security.CurrentTorRoute.currentSocksPort
      if (port != null && port > 0) {
        builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)))
      }
    }
    return builder.build()
  }

  /**
   * Translates a single text string to target language
   */
  suspend fun translateText(text: String, targetLanguageCode: String, isGhost: Boolean = false): String = withContext(Dispatchers.IO) {
    if (text.isBlank()) return@withContext ""
    try {
      val encoded = URLEncoder.encode(text, "UTF-8")
      val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLanguageCode&dt=t&q=$encoded"

      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "Mozilla/5.0")
        .build()

      val response = getClient(isGhost).newCall(request).execute()
      if (!response.isSuccessful) return@withContext text

      val body = response.body?.string() ?: return@withContext text
      val jsonArray = JSONArray(body)
      val sentencesArray = jsonArray.optJSONArray(0) ?: return@withContext text

      val sb = StringBuilder()
      for (i in 0 until sentencesArray.length()) {
        val sentenceObj = sentencesArray.optJSONArray(i)
        if (sentenceObj != null && sentenceObj.length() > 0) {
          sb.append(sentenceObj.optString(0))
        }
      }
      val result = sb.toString().trim()
      if (result.isNotBlank()) result else text
    } catch (e: Exception) {
      Log.e(TAG, "Translation error for language $targetLanguageCode", e)
      text
    }
  }

  /**
   * Translates full ReaderArticle and returns new article instance with translated content
   */
  suspend fun translateArticle(
    article: ReaderArticle,
    targetLanguageCode: String,
    isGhost: Boolean = false,
    onProgress: (Int, Int) -> Unit = { _, _ -> }
  ): ReaderArticle = withContext(Dispatchers.IO) {
    val lang = SUPPORTED_LANGUAGES.firstOrNull { it.code == targetLanguageCode }?.displayName ?: targetLanguageCode

    val translatedTitle = translateText(article.title, targetLanguageCode, isGhost)
    val translatedParas = mutableListOf<ReaderParagraph>()

    val total = article.paragraphs.size
    for ((idx, p) in article.paragraphs.withIndex()) {
      onProgress(idx + 1, total)
      val translatedText = translateText(p.text, targetLanguageCode, isGhost)
      translatedParas.add(
        p.copy(text = translatedText)
      )
    }

    article.copy(
      translatedTitle = translatedTitle,
      translatedParagraphs = translatedParas,
      targetLanguage = lang,
    )
  }

  /**
   * Launch external Google Translate intent for a web URL or text snippet
   */
  fun launchExternalTranslator(context: Context, urlOrText: String) {
    try {
      val targetUri = if (urlOrText.startsWith("http://") || urlOrText.startsWith("https://")) {
        Uri.parse("https://translate.google.com/translate?sl=auto&tl=hi&u=${URLEncoder.encode(urlOrText, "UTF-8")}")
      } else {
        Uri.parse("https://translate.google.com/?sl=auto&tl=hi&text=${URLEncoder.encode(urlOrText, "UTF-8")}&op=translate")
      }
      val intent = Intent(Intent.ACTION_VIEW, targetUri)
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to launch external translator", e)
    }
  }
}
