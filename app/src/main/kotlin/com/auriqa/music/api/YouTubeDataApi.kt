package com.auriqo.music.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PlaylistAttribution(
    val channelId: String,
    val channelTitle: String,
    val addedAt: String?,
)

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
    )

    @Serializable
    private data class ResourceId(val videoId: String? = null)

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
                )
            }
            pageToken = response.nextPageToken
        } while (pageToken != null)
        result
    }
}
