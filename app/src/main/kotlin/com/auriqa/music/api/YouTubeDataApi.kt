package com.auriqo.music.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PlaylistAttribution(
    val channelId: String,
    val channelTitle: String,
    val addedAt: String?,
    val avatarUrl: String? = null,
)

fun formatPlaylistAttributionDate(value: String?): String? = value?.let {
    runCatching {
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
            .format(Instant.parse(it).atZone(ZoneId.systemDefault()))
    }.getOrNull()
}

object YouTubeDataApi {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    @Serializable
    private data class Response(
        val items: List<Item> = emptyList(),
        val nextPageToken: String? = null,
    )

    @Serializable
    private data class Item(
        val snippet: Snippet? = null,
    )

    @Serializable
    private data class Snippet(
        val publishedAt: String? = null,
        val channelId: String? = null,
        val channelTitle: String? = null,
        val resourceId: ResourceId? = null,
        val thumbnails: Map<String, Thumbnail>? = null,
    )

    @Serializable
    private data class Thumbnail(val url: String? = null)

    @Serializable
    private data class ResourceId(val videoId: String? = null)

    @Serializable
    private data class WorkerAttribution(
        val channelId: String,
        val channelTitle: String,
        val addedAt: String? = null,
        val avatarUrl: String? = null,
    )

    @Serializable
    private data class WorkerResponse(
        val items: Map<String, WorkerAttribution> = emptyMap(),
    )

    suspend fun workerPlaylistAttributions(
        workerUrl: String,
        playlistId: String,
        accessToken: String? = null,
    ): Result<Map<String, PlaylistAttribution>> = runCatching {
        val response = client.get(workerUrl.trimEnd('/') + "/v1/playlist-attributions") {
            parameter("playlistId", playlistId)
            accessToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
        }.body<WorkerResponse>()
        response.items.mapValues { (_, item) ->
            PlaylistAttribution(item.channelId, item.channelTitle, item.addedAt, item.avatarUrl)
        }
    }

    suspend fun playlistAttributions(apiKey: String, playlistId: String): Result<Map<String, PlaylistAttribution>> = runCatching {
        require(apiKey.isNotBlank())
        val result = linkedMapOf<String, PlaylistAttribution>()
        var pageToken: String? = null
        do {
            val response = client.get("https://www.googleapis.com/youtube/v3/playlistItems") {
                parameter("part", "snippet")
                parameter("playlistId", playlistId)
                parameter("maxResults", 50)
                parameter("key", apiKey)
                pageToken?.let { parameter("pageToken", it) }
            }.body<Response>()
            response.items.forEach { item ->
                val snippet = item.snippet ?: return@forEach
                val videoId = snippet.resourceId?.videoId ?: return@forEach
                val channelId = snippet.channelId ?: return@forEach
                result[videoId] = PlaylistAttribution(
                    channelId = channelId,
                    channelTitle = snippet.channelTitle.orEmpty().ifBlank { channelId },
                    addedAt = snippet.publishedAt,
                    avatarUrl = snippet.thumbnails?.get("default")?.url,
                )
            }
            pageToken = response.nextPageToken
        } while (pageToken != null)
        result
    }
}
