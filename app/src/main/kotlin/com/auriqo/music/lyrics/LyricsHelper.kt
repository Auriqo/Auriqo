

package com.auriqo.music.lyrics

import android.content.Context
import com.auriqo.music.constants.LyricsProviderOrderKey
import com.auriqo.music.constants.PreferredLyricsProvider
import com.auriqo.music.constants.PreferredLyricsProviderKey
import com.auriqo.music.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.auriqo.music.extensions.toEnum
import com.auriqo.music.models.MediaMetadata
import com.auriqo.music.utils.NetworkConnectivityObserver
import com.auriqo.music.utils.dataStore
import com.auriqo.music.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    
    private suspend fun resolveLyricsProviders(): List<LyricsProvider> {
        val preferences = context.dataStore.data.first()
        val orderString = preferences[LyricsProviderOrderKey].orEmpty()

        if (orderString.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(orderString)
        }

        
        val preferredEnum = preferences[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.YOULYPLUS)
        val preferredName = LyricsProviderRegistry.getProviderNameForEnum(preferredEnum)
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val migratedOrder = listOf(preferredName) + defaultOrder.filter { it != preferredName }
        return migratedOrder.mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
    }



    private val cache = LyricsResultCache(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return LyricsWithProvider(cached.lyrics, cached.providerName)
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }

        val providers = resolveLyricsProviders().filter { it.isEnabled(context) }
        if (providers.isEmpty()) return LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")

        return coroutineScope {
            val channel = Channel<LyricsWithProvider?>(providers.size)
            providers.forEach { provider ->
                launch {
                    try {
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                            mediaMetadata.album?.title,
                        )
                        result.onSuccess { lyrics ->
                            if (lyrics != LYRICS_NOT_FOUND && lyrics.isNotBlank()) {
                                channel.send(LyricsWithProvider(lyrics, provider.name))
                            } else {
                                channel.send(null)
                            }
                        }.onFailure {
                            reportException(it)
                            channel.send(null)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                        channel.send(null)
                    }
                }
            }

            var responses = 0
            val receivedResults = mutableListOf<LyricsWithProvider>()

            val synced = withTimeoutOrNull(UNSYNCED_WAIT_TIMEOUT_MS) {
                while (responses < providers.size) {
                    val result = channel.receive()
                    responses++
                    if (result != null) {
                        receivedResults += result
                        val isSynced = result.lyrics.trimStart().startsWith("[")
                        if (isSynced) {
                            return@withTimeoutOrNull result
                        }
                    }
                }
                null
            }
            coroutineContext.cancelChildren()
            return@coroutineScope synced
                ?: LyricsFallbackPolicy.select(receivedResults)
                ?: LyricsWithProvider(LYRICS_NOT_FOUND, "Unknown")
        }
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        cache.get(mediaId)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return
        }

        val allResult = java.util.concurrent.CopyOnWriteArrayList<LyricsResult>()
        val providers = resolveLyricsProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob()).launch {
            val jobs = providers.mapNotNull { provider ->
                if (provider.isEnabled(context)) {
                    launch {
                        try {
                            provider.getAllLyrics(mediaId, songTitle, songArtists, duration, album) { lyrics ->
                                val result = LyricsResult(provider.name, lyrics)
                                allResult += result
                                callback(result)
                            }
                        } catch (e: Exception) {
                            reportException(e)
                        }
                    }
                } else null
            }
            jobs.forEach { it.join() }
            cache.put(mediaId, allResult.toList())
        }

        currentLyricsJob?.join()
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 30
        private const val UNSYNCED_WAIT_TIMEOUT_MS = 6000L
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)

/** Selects the preferred result after provider requests complete or time out. */
internal object LyricsFallbackPolicy {
    fun select(results: List<LyricsWithProvider>): LyricsWithProvider? =
        results.firstOrNull { it.lyrics.trimStart().startsWith("[") } ?: results.firstOrNull()
}

/** Small access-ordered cache that bounds retained provider responses by media id. */
internal class LyricsResultCache(private val maxEntries: Int) {
    private val entries = object : LinkedHashMap<String, List<LyricsResult>>(maxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<LyricsResult>>?): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun get(mediaId: String): List<LyricsResult>? = entries[mediaId]

    @Synchronized
    fun put(mediaId: String, results: List<LyricsResult>) {
        entries[mediaId] = results
    }
}
