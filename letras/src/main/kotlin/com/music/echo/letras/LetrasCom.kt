package com.music.echo.letras

import com.music.echo.letras.models.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import org.jsoup.Jsoup

object LetrasCom {
    private const val BASE_URL = "https://www.letras.com"

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
                header(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36",
                )
            }

            expectSuccess = false
        }
    }

    suspend fun search(
        title: String,
        artist: String,
    ): List<SearchResult> = runCatching {
        val query = "$artist $title"
        val response = client.get("$BASE_URL/search/") {
            url {
                parameters.append("q", query)
            }
        }

        val html = response.bodyAsText()
        parseSearchResults(html)
    }.getOrDefault(emptyList())

    suspend fun getLyrics(url: String): Result<String> = runCatching {
        val response = client.get(url)

        val html = response.bodyAsText()
        parseLyricsFromHtml(html)
            ?: throw IllegalStateException("Lyrics not found on page")
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
    ): Result<String> {
        val results = search(title, artist)

        if (results.isEmpty()) {
            return Result.failure(IllegalStateException("No search results found"))
        }

        val bestMatch = findBestMatch(results, title, artist)
            ?: results.firstOrNull()
            ?: return Result.failure(IllegalStateException("No matching result found"))

        val lyricsUrl = bestMatch.url
        return if (!lyricsUrl.startsWith("http")) {
            getLyrics(BASE_URL + lyricsUrl)
        } else {
            getLyrics(lyricsUrl)
        }
    }

    private fun parseSearchResults(html: String): List<SearchResult> {
        val doc = Jsoup.parse(html)
        val results = mutableListOf<SearchResult>()

        doc.select("a[href*=/letras/]").forEach { element ->
            val href = element.attr("href")
            val text = element.text().trim()

            if (text.isBlank() || !href.contains("/letras/")) return@forEach

            val parts = text.split(" - ", limit = 2)
            val (artist, title) = if (parts.size == 2) {
                parts[0].trim() to parts[1].trim()
            } else {
                parseArtistTitleFromUrl(href)
            }

            if (artist.isNotBlank() && title.isNotBlank()) {
                results.add(SearchResult(url = href, title = title, artist = artist))
            }
        }

        if (results.isEmpty()) {
            doc.select("li a[href]").forEach { element ->
                val href = element.attr("href")
                val text = element.text().trim()

                if (text.isBlank() || !href.contains("/")) return@forEach

                val parts = text.split(" - ", limit = 2)
                if (parts.size == 2) {
                    results.add(
                        SearchResult(
                            url = href,
                            title = parts[1].trim(),
                            artist = parts[0].trim(),
                        ),
                    )
                }
            }
        }

        return results
    }

    private fun parseArtistTitleFromUrl(url: String): Pair<String, String> {
        val cleanPath = url.removePrefix(BASE_URL).trim('/')
        val segments = cleanPath.split("/")
        return if (segments.size >= 2) {
            val artistSlug = segments[0].replace("-", " ")
            val titleSlug = segments[1].replace("-", " ")
            artistSlug to titleSlug
        } else {
            "" to ""
        }
    }

    private fun findBestMatch(
        results: List<SearchResult>,
        title: String,
        artist: String,
    ): SearchResult? {
        val normTitle = normalize(title)
        val normArtist = normalize(artist)

        return results.maxByOrNull { result ->
            val resTitle = normalize(result.title)
            val resArtist = normalize(result.artist)
            var score = 0

            if (resTitle.equals(normTitle, ignoreCase = true)) {
                score += 2
            } else if (resTitle.contains(normTitle, ignoreCase = true) ||
                normTitle.contains(resTitle, ignoreCase = true)
            ) {
                score += 1
            }

            if (resArtist.equals(normArtist, ignoreCase = true)) {
                score += 2
            } else if (resArtist.contains(normArtist, ignoreCase = true) ||
                normArtist.contains(resArtist, ignoreCase = true)
            ) {
                score += 1
            }

            score
        }
    }

    private fun normalize(text: String): String = text
        .lowercase()
        .replace(Regex("\\(.*?\\)"), "")
        .replace(Regex("\\[.*?\\]"), "")
        .replace(Regex("【.*?】"), "")
        .replace(
            Regex(
                "\\b(official|video|audio|lyrics|visualizer|hd|hq|" +
                    "remaster|remix|live|acoustic|version|edit|" +
                    "original|studio|session|extended|radio|single|" +
                    "album|cover|mix|feat\\.?|ft\\.?|featuring)" +
                    "\\b",
                RegexOption.IGNORE_CASE,
            ),
            "",
        )
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun parseLyricsFromHtml(html: String): String? {
        val doc = Jsoup.parse(html)

        val selectors = listOf(
            "div.lyric-original",
            "div.cnt-letra",
            "div.cnt-letra p",
            "div.lyrics",
            "div.letra",
            "article p",
        )

        for (selector in selectors) {
            val elements = doc.select(selector)
            if (elements.isNotEmpty()) {
                val lyrics = elements.joinToString("\n") { it.text().trim() }
                    .replace(Regex("\n{3,}"), "\n\n")
                    .trim()

                if (lyrics.length > 20) {
                    return lyrics
                }
            }
        }

        return null
    }
}
