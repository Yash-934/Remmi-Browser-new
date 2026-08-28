package com.remmi.browser.reader

import android.content.Context
import android.util.Log
import com.remmi.browser.security.CurrentTorRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit

data class ReaderParagraph(
  val index: Int,
  val text: String,
  val isHeading: Boolean = false,
  val headingLevel: Int = 0,
)

data class ReaderHighlight(
  val paragraphIndex: Int,
  val colorHex: String = "#FFEB3B", // Default yellow
  val isUnderline: Boolean = false,
  val note: String = "",
)

data class ReaderArticle(
  val title: String,
  val byline: String = "",
  val siteName: String = "",
  val excerpt: String = "",
  val paragraphs: List<ReaderParagraph> = emptyList(),
  val rawTextList: List<String> = emptyList(),
  val readingTimeMinutes: Int = 1,
  val sourceUrl: String = "",
  val leadImageUrl: String? = null,
  val translatedTitle: String? = null,
  val translatedParagraphs: List<ReaderParagraph>? = null,
  val targetLanguage: String? = null,
) {
  val fullPlainText: String
    get() = paragraphs.joinToString("\n\n") { it.text }

  val activeParagraphs: List<ReaderParagraph>
    get() = translatedParagraphs ?: paragraphs

  val activeTitle: String
    get() = translatedTitle ?: title
}

object ReaderExtractor {
  private const val TAG = "ReaderExtractor"

  private const val MAX_RESPONSE_BYTES = 2L * 1024L * 1024L // 2 MB cap
  private const val MAX_ARTICLE_PARAGRAPHS = 500

  private fun getClient(isGhost: Boolean, url: String? = null): OkHttpClient {
    return com.remmi.browser.security.NetworkRouteAuthority.createHttpClient(
      isGhost = isGhost,
      targetUrl = url,
      connectTimeoutSeconds = 10L,
      readTimeoutSeconds = 15L,
      followRedirects = true
    )
  }

  /**
   * Fetches the web page asynchronously and extracts full clean article content
   */
  suspend fun extractFromUrl(url: String, currentTitle: String = "", isGhost: Boolean = false): ReaderArticle = withContext(Dispatchers.IO) {
    val domain = try {
      URI(url).host ?: url.substringAfter("://").substringBefore('/')
    } catch (e: Exception) {
      url.substringAfter("://").substringBefore('/')
    }

    // SSRF and Scheme Gate
    if (!com.remmi.browser.security.RedirectInspector.isSchemeSafeForNavigation(url)) {
      return@withContext createFallbackArticle(url, currentTitle, domain, "Extraction blocked: Unsupported or unsafe URL scheme")
    }

    if (com.remmi.browser.security.NavigationSecurityAuthority.isPrivateOrLocalHost(domain)) {
      return@withContext createFallbackArticle(url, currentTitle, domain, "Extraction blocked: Local/Private address targets are prohibited")
    }

    val isOnion = com.remmi.browser.security.NetworkRouteAuthority.isOnionDestination(url)
    if ((isGhost || isOnion) && !com.remmi.browser.security.CurrentTorRoute.isReady) {
      return@withContext createFallbackArticle(url, currentTitle, domain, "Extraction blocked: Tor route is not verified")
    }

    val client = try {
      getClient(isGhost || isOnion, url)
    } catch (e: Exception) {
      return@withContext createFallbackArticle(url, currentTitle, domain, "Network authority error: ${e.message}")
    }

    try {
      kotlinx.coroutines.withTimeout(20_000L) {
        val request = Request.Builder()
          .url(url)
          .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
          .header("Accept", "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.8")
          .build()

        val call = client.newCall(request)
        // Coroutine cancellation support
        val response = try {
          call.execute()
        } catch (e: Exception) {
          call.cancel()
          throw e
        }

        response.use { resp ->
          if (!resp.isSuccessful) {
            return@withTimeout createFallbackArticle(url, currentTitle, domain, "HTTP error ${resp.code}")
          }

          val contentType = resp.header("Content-Type", "")?.lowercase() ?: ""
          if (contentType.isNotEmpty() && !contentType.contains("text/html") && !contentType.contains("xhtml") && !contentType.contains("text/plain")) {
            return@withTimeout createFallbackArticle(url, currentTitle, domain, "Unsupported Content-Type: $contentType")
          }

          val responseBody = resp.body
          if (responseBody == null) {
            return@withTimeout createFallbackArticle(url, currentTitle, domain, "Empty page response")
          }

          val source = responseBody.source()
          val bytes = source.readByteArray(MAX_RESPONSE_BYTES)
          val html = String(bytes, Charsets.UTF_8)

          if (html.isBlank()) {
            return@withTimeout createFallbackArticle(url, currentTitle, domain, "Empty page response")
          }

          parseHtmlDocument(html, url, currentTitle, domain)
        }
      }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
      Log.e(TAG, "Reader extraction timed out for $url", e)
      createFallbackArticle(url, currentTitle, domain, "Extraction timed out (20s limit)")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to extract article from $url", e)
      createFallbackArticle(url, currentTitle, domain, e.localizedMessage ?: "Extraction error")
    }
  }

  fun parseHtmlDocument(
    html: String,
    url: String,
    fallbackTitle: String,
    domain: String,
  ): ReaderArticle {
    val boundedHtml = if (html.length > 2_000_000) html.take(2_000_000) else html
    val doc: Document = Jsoup.parse(boundedHtml, url)

    // Extract Title
    val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()
    val twitterTitle = doc.selectFirst("meta[name=twitter:title]")?.attr("content")?.trim()
    val h1Title = doc.selectFirst("h1")?.text()?.trim()
    val docTitle = doc.title().trim()

    val title = listOfNotNull(ogTitle, twitterTitle, h1Title, docTitle, fallbackTitle)
      .firstOrNull { it.isNotBlank() } ?: "Reader Mode Article"

    // Extract Author / Byline
    val author = doc.selectFirst("meta[name=author]")?.attr("content")?.trim()
      ?: doc.selectFirst("meta[property=article:author]")?.attr("content")?.trim()
      ?: doc.selectFirst("[rel=author], .byline, .author, .by-line, .entry-author")?.text()?.trim()
      ?: ""

    // Extract Site Name
    val siteName = doc.selectFirst("meta[property=og:site_name]")?.attr("content")?.trim()
      ?: domain.removePrefix("www.")

    // Lead Image
    val leadImage = doc.selectFirst("meta[property=og:image]")?.attr("content")
      ?: doc.selectFirst("meta[name=twitter:image]")?.attr("content")

    // Find main article container
    val contentContainer: Element = doc.selectFirst(".mw-parser-output") // Wikipedia
      ?: doc.selectFirst("article")
      ?: doc.selectFirst("[role=main]")
      ?: doc.selectFirst(".post-content, .entry-content, .article-content, .story-body, .article-body, #article-body, #content")
      ?: doc.body()

    // Clone to manipulate without altering original doc
    val cleanContainer = contentContainer.clone()

    // Remove noise elements
    val noiseSelectors = listOf(
      "script", "style", "noscript", "nav", "header", "footer", "iframe", "aside",
      ".ad", ".ads", ".advertisement", ".social-share", ".share-box", ".comments",
      ".sidebar", ".menu", ".nav", ".cookie-banner", ".banner", ".mw-editsection",
      ".reflist", ".reference", ".infobox", ".navbox", ".hatnote", ".noprint",
      ".metadata", ".thumbcaption", "form", "button", "input"
    )
    for (sel in noiseSelectors) {
      cleanContainer.select(sel).remove()
    }

    // Extract paragraphs and headings using aggressive newline-preserving extraction
    // This forces Read Mode to work on ALL websites even if they lack standard <p> tags.
    cleanContainer.select("br").append(" _NEWLINE_ ")
    cleanContainer.select("p, h1, h2, h3, h4, h5, h6, blockquote, li").prepend(" _NEWLINE_ _NEWLINE_ ")
    cleanContainer.select("div, section, article").prepend(" _NEWLINE_ ")

    var bodyText = cleanContainer.text()
    // Replace the newline markers with actual newlines, accounting for possible spaces Jsoup adds
    bodyText = bodyText.replace(Regex("\\s*_NEWLINE_\\s*"), "\n").trim()
    // Collapse 3+ newlines into 2
    bodyText = bodyText.replace(Regex("\n{3,}"), "\n\n")

    val chunks = bodyText.split(Regex("\n\\s*\n"))
      .filter { it.trim().length > 15 }
      .take(MAX_ARTICLE_PARAGRAPHS)

    val paragraphs = mutableListOf<ReaderParagraph>()
    val rawList = mutableListOf<String>()

    chunks.forEachIndexed { idx, chunk ->
      val text = chunk.trim().take(10_000)
      // Basic heading detection: short, no terminal punctuation
      val isHeading = text.length in 5..80 && !text.matches(Regex(".*[.?!]$"))
      val headingLevel = if (isHeading) 2 else 0

      paragraphs.add(
        ReaderParagraph(
          index = idx,
          text = text,
          isHeading = isHeading,
          headingLevel = headingLevel,
        )
      )
      rawList.add(text)
    }

    if (paragraphs.isEmpty()) {
      val backup = doc.body().text().trim()
      if (backup.isNotBlank()) {
        paragraphs.add(ReaderParagraph(0, backup))
        rawList.add(backup)
      }
    }

    val totalWords = rawList.sumOf { it.split("\\s+".toRegex()).size }
    val readingTime = Math.max(1, Math.ceil(totalWords / 200.0).toInt())

    return ReaderArticle(
      title = title,
      byline = author,
      siteName = siteName,
      paragraphs = paragraphs,
      rawTextList = rawList,
      readingTimeMinutes = readingTime,
      sourceUrl = url,
      leadImageUrl = leadImage,
    )
  }

  private fun createFallbackArticle(
    url: String,
    title: String,
    domain: String,
    note: String
  ): ReaderArticle {
    val cleanTitle = title.ifBlank { "Article on $domain" }
    val sampleParas = listOf(
      ReaderParagraph(0, "Live reader mode extracted from $domain."),
      ReaderParagraph(1, "Original source: $url"),
      ReaderParagraph(2, "Status note: $note")
    )
    return ReaderArticle(
      title = cleanTitle,
      siteName = domain,
      paragraphs = sampleParas,
      rawTextList = sampleParas.map { it.text },
      readingTimeMinutes = 1,
      sourceUrl = url,
    )
  }

  fun parseArticle(json: JSONObject, fallbackUrl: String = ""): ReaderArticle? {
    val title = json.optString("title").ifBlank { "Untitled Article" }
    val byline = json.optString("byline")
    val siteName = json.optString("siteName")
    val readTime = json.optInt("readingTimeMinutes", 1)
    val sourceUrl = json.optString("sourceUrl", fallbackUrl)
    val jsonArray = json.optJSONArray("paragraphs")
    val paragraphs = mutableListOf<ReaderParagraph>()
    val rawList = mutableListOf<String>()

    if (jsonArray != null) {
      for (i in 0 until jsonArray.length()) {
        val p = jsonArray.optString(i)
        if (p.isNotBlank()) {
          paragraphs.add(ReaderParagraph(index = i, text = p))
          rawList.add(p)
        }
      }
    }
    if (paragraphs.isEmpty()) return null
    return ReaderArticle(
      title = title,
      byline = byline,
      siteName = siteName,
      paragraphs = paragraphs,
      rawTextList = rawList,
      readingTimeMinutes = readTime,
      sourceUrl = sourceUrl,
    )
  }
}

