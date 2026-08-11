package com.music.echo.letras

import com.music.echo.letras.models.SearchResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.text.Normalizer
import java.util.Locale

object LetrasCom {
    private const val BASE_URL = "https://www.letras.com"
    private const val SEARCH_URL = "https://solr.sscdn.co/letras/m1/"
    private const val JSONP_CALLBACK = "LetrasSug"

    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }

            defaultRequest {
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
                header(
                    HttpHeaders.UserAgent,
                    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36",
                )
            }

            expectSuccess = false
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
    ): Result<String> = try {
        val results = search(title, artist)
        val bestMatch = findBestMatch(results, title, artist)
            ?: return Result.failure(IllegalStateException("No unambiguous Letras.com match found"))

        getLyricsFromUrl(bestMatch.url)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private suspend fun search(title: String, artist: String): List<SearchResult> {
        val response = client.get(SEARCH_URL) {
            parameter("q", "$artist $title")
            parameter("wt", "json")
            parameter("callback", JSONP_CALLBACK)
        }
        check(response.status.isSuccess()) { "Letras.com search failed with ${response.status}" }
        return parseSearchResults(response.bodyAsText())
    }

    private suspend fun getLyricsFromUrl(url: String): Result<String> {
        if (!url.startsWith("$BASE_URL/")) {
            return Result.failure(IllegalArgumentException("Unexpected Letras.com URL"))
        }

        return try {
            val response = client.get(url)
            check(response.status.isSuccess()) { "Letras.com lyrics request failed with ${response.status}" }
            parseLyricsFromHtml(response.bodyAsText())
                ?.let(Result.Companion::success)
                ?: Result.failure(IllegalStateException("Lyrics not found on page"))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    internal fun parseSearchResults(payload: String): List<SearchResult> {
        val jsonPayload = payload
            .trim()
            .removePrefix("$JSONP_CALLBACK(")
            .removeSuffix(")")

        val documents = json.parseToJsonElement(jsonPayload)
            .jsonObject["response"]
            ?.jsonObject
            ?.get("docs")
            ?.jsonArray
            ?: return emptyList()

        return documents.mapNotNull { document ->
            val fields = document.jsonObject
            if (fields["t"]?.jsonPrimitive?.content != "2") return@mapNotNull null

            val title = fields["txt"]?.jsonPrimitive?.content?.trim().orEmpty()
            val artist = fields["art"]?.jsonPrimitive?.content?.trim().orEmpty()
            val artistSlug = fields["dns"]?.jsonPrimitive?.content?.trim().orEmpty()
            val songSlug = fields["url"]?.jsonPrimitive?.content?.trim().orEmpty()
            if (title.isBlank() || artist.isBlank() || !isPathSegment(artistSlug) || !isPathSegment(songSlug)) {
                return@mapNotNull null
            }

            SearchResult(
                url = "$BASE_URL/$artistSlug/$songSlug/",
                title = title,
                artist = artist,
            )
        }
    }

    internal fun findBestMatch(
        results: List<SearchResult>,
        title: String,
        artist: String,
    ): SearchResult? {
        val normalizedTitle = normalizeTitle(title)
        val requestedArtists = artistVariants(artist)
        if (normalizedTitle.isBlank() || requestedArtists.isEmpty()) return null

        return results
            .filter { normalizeTitle(it.title) == normalizedTitle }
            .maxByOrNull { result -> artistMatchScore(requestedArtists, normalizeArtist(result.artist)) }
            ?.takeIf { artistMatchScore(requestedArtists, normalizeArtist(it.artist)) > 0 }
    }

    internal fun parseLyricsFromHtml(html: String): String? {
        val paragraphs = Jsoup.parse(html)
            .select("#js-lyric-content .lyric-content > p, div.lyric-content > p")
            .map(::paragraphText)
            .filter { it.isNotBlank() }

        return paragraphs.joinToString("\n\n").trim().takeIf { it.isNotBlank() }
    }

    private fun paragraphText(paragraph: org.jsoup.nodes.Element): String {
        val htmlWithLineBreaks = paragraph.html().replace(BR_TAG, "\n")
        return Jsoup.parseBodyFragment(htmlWithLineBreaks)
            .body()
            .wholeText()
            .replace("\r\n", "\n")
            .lines()
            .joinToString("\n") { it.trim() }
            .trim()
    }

    private fun artistMatchScore(requestedArtists: Set<String>, resultArtist: String): Int = when {
        resultArtist in requestedArtists && requestedArtists.size == 1 -> 2
        resultArtist in requestedArtists -> 1
        else -> 0
    }

    private fun artistVariants(value: String): Set<String> = value
        .split(RAW_ARTIST_SEPARATOR)
        .map(::normalizeArtist)
        .filter { it.isNotBlank() }
        .toSet()

    private fun normalizeTitle(value: String): String = normalize(value)
        .replace(DECORATION_SUFFIX, "")
        .replace(FEATURING_SUFFIX, "")
        .trim()

    private fun normalizeArtist(value: String): String = normalize(value)
        .replace(FEATURING_SUFFIX, "")
        .trim()

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace(PUNCTUATION, " ")
        .replace(WHITESPACE, " ")
        .trim()

    private fun isPathSegment(value: String): Boolean =
        value.isNotBlank() && value.none { it == '/' || it == '?' || it == '#' }

    private val BR_TAG = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val COMBINING_MARKS = Regex("\\p{M}+")
    private val PUNCTUATION = Regex("[^\\p{L}\\p{N}]+")
    private val WHITESPACE = Regex("\\s+")
    private val DECORATION_SUFFIX = Regex(
        """\s+(?:official(?: music)? video|official audio|audio|lyrics?|lyric video|visuali[sz]er|hd|hq|4k)$""",
    )
    private val FEATURING_SUFFIX = Regex(
        """\s+(?:feat(?:uring)?|ft)\s+.+$""",
    )
    private val RAW_ARTIST_SEPARATOR = Regex(
        """\s*(?:,|&|\band\b|\bx\b|\bfeat(?:\.|uring)?\b|\bft\.?)\s*""",
        RegexOption.IGNORE_CASE,
    )
}
