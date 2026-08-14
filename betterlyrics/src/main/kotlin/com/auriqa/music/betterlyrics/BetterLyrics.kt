package com.auriqo.music.betterlyrics

import com.auriqo.music.betterlyrics.models.TTMLResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object BetterLyrics {
    private const val USER_AGENT = "Auriqo/1.0 (Android) betterlyrics-client"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url("https://lyrics-api.boidu.dev")
                header(HttpHeaders.UserAgent, USER_AGENT)
            }

            expectSuccess = false
        }
    }

    private suspend fun fetchTTML(
        artist: String,
        title: String,
        durationMs: Int = -1,
        album: String? = null,
        videoId: String? = null,
    ): Result<String> = runCatching {
        val response = client.get("/getLyrics") {
            parameter("s", title)
            parameter("a", artist)
            if (durationMs > 0) {
                parameter("d", durationMs)
            }
            if (!album.isNullOrBlank()) {
                parameter("al", album)
            }
            if (!videoId.isNullOrBlank()) {
                parameter("v", videoId)
            }
        }
        when (response.status) {
            HttpStatusCode.OK -> response.body<TTMLResponse>().ttml
            HttpStatusCode.Unauthorized ->
                throw IllegalStateException("API key required for uncached lyrics")
            HttpStatusCode.NotFound ->
                throw IllegalStateException("No lyrics available for this track")
            HttpStatusCode.TooManyRequests ->
                throw IllegalStateException("BetterLyrics rate limited")
            else ->
                throw IllegalStateException("BetterLyrics HTTP ${response.status.value}")
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
    ) = runCatching {
        // Use exact title and artist - no normalization to ensure correct sync
        // Normalizing can return wrong lyrics (e.g., radio edit vs original)
        // Duration is passed in seconds but the API expects milliseconds.
        val durationMs = if (duration > 0) duration * 1000 else -1

        val ttml = fetchTTML(artist, title, durationMs, album, videoId).getOrThrow()

        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) {
            throw IllegalStateException("Failed to parse lyrics")
        }

        TTMLParser.toLRC(parsedLines)
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        videoId: String? = null,
        callback: (String) -> Unit,
    ) {
        getLyrics(title, artist, duration, album, videoId)
            .onSuccess { lrcString ->
                callback(lrcString)
            }
    }
}
