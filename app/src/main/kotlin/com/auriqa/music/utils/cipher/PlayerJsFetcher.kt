package com.auriqo.music.utils.cipher

import com.music.innertube.YouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File

object PlayerJsFetcher {
    private const val TAG = "echomusic_CipherFetcher"
    private const val IFRAME_API_URL = "https://www.youtube.com/iframe_api"
    private const val EMBED_URL_TEMPLATE = "https://www.youtube.com/embed/%s"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L 

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    
    private val cacheMutex = Mutex()

    private data class PlayerJsSource(
        val hash: String,
        val url: String,
        val origin: String,
    )

    private fun getCacheDir(): File = File(CipherDeobfuscator.appContext.filesDir, "cipher_cache")

    private fun getCacheFile(hash: String): File = File(getCacheDir(), "player_$hash.js")

    private fun getHashFile(): File = File(getCacheDir(), "current_hash.txt")

    suspend fun getPlayerJs(
        videoId: String? = null,
        forceRefresh: Boolean = false,
    ): Pair<String, String>? = cacheMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = getCacheDir()
                if (!cacheDir.exists()) cacheDir.mkdirs()


                if (!forceRefresh) {
                    val cached = readFromCache()
                    if (cached != null) {
                        Timber.tag(TAG).d("Using cached player JS (hash=${cached.second})")
                        return@withContext cached
                    }
                }


                val sources = fetchPlayerSources(videoId)
                if (sources.isEmpty()) {
                    Timber.tag(TAG).e("Could not discover a usable YouTube player JS URL")
                    return@withContext null
                }

                for (source in sources) {
                    Timber.tag(TAG).d("Trying player JS from ${source.origin}: hash=${source.hash}")
                    val playerJs = downloadPlayerJs(source.url)
                    if (playerJs == null) {
                        Timber.tag(TAG).w("Failed to download player JS from ${source.origin}")
                        continue
                    }

                    Timber.tag(TAG).d("Downloaded player JS: ${playerJs.length} chars")
                    writeToCache(source.hash, playerJs)
                    return@withContext Pair(playerJs, source.hash)
                }

                Timber.tag(TAG).e("Failed to download all discovered player JS candidates")
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "getPlayerJs exception: ${e.message}")
                null
            }
        }
    }

    suspend fun invalidateCache() = cacheMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = getCacheDir()
                if (cacheDir.exists()) {
                    val files = cacheDir.listFiles()?.filter {
                        it.name.startsWith("player_") || it.name == "current_hash.txt"
                    }
                    files?.forEach { it.delete() }
                }
                Timber.tag(TAG).d("Cache invalidated")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to invalidate cache: ${e.message}")
            }
        }
    }

    private fun readFromCache(): Pair<String, String>? {
        try {
            val hashFile = getHashFile()
            if (!hashFile.exists()) return null

            val hashData = hashFile.readText().split("\n")
            if (hashData.size < 2) return null

            val hash = hashData[0]
            val timestamp = hashData[1].toLongOrNull() ?: return null

            
            if (System.currentTimeMillis() - timestamp !in 0..<CACHE_TTL_MS) {
                Timber.tag(TAG).d("Cache expired or clock skew detected (hash=$hash)")
                return null
            }

            val cacheFile = getCacheFile(hash)
            if (!cacheFile.exists()) return null

            val playerJs = cacheFile.readText()
            if (playerJs.isEmpty()) return null

            return Pair(playerJs, hash)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading cache: ${e.message}")
            return null
        }
    }

    private fun writeToCache(hash: String, playerJs: String) {
        try {
            val cacheDir = getCacheDir()
            
            cacheDir.listFiles()?.filter { it.name.startsWith("player_") }?.forEach { it.delete() }

            PlayerConfigStore.writeAtomic(getCacheFile(hash), playerJs)
            PlayerConfigStore.writeAtomic(getHashFile(), "$hash\n${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error writing cache: ${e.message}")
        }
    }

    private fun fetchPlayerSources(videoId: String?): List<PlayerJsSource> {
        val sources = mutableListOf<PlayerJsSource>()

        fetchText(IFRAME_API_URL)?.let { body ->
            val url = PlayerJsUrlParser.fromIframeApi(body)
            if (url != null) {
                extractSource(url, "iframe_api")?.let(sources::add)
            } else {
                Timber.tag(TAG).w("iframe_api did not expose a base player URL")
            }
        }

        if (!videoId.isNullOrBlank()) {
            val embedUrl = EMBED_URL_TEMPLATE.format(videoId)
            fetchText(embedUrl)?.let { body ->
                val url = PlayerJsUrlParser.fromEmbedPage(body)
                if (url != null) {
                    extractSource(url, "embed/$videoId")?.let(sources::add)
                } else {
                    Timber.tag(TAG).w("Embed page did not expose a base player URL for $videoId")
                }
            }
        }

        return sources.distinctBy { it.url }
    }

    private fun extractSource(url: String, origin: String): PlayerJsSource? {
        val hash = Regex("""/s/player/([A-Za-z0-9_-]{8})/""")
            .find(url)
            ?.groupValues
            ?.get(1)
            ?: return null
        return PlayerJsSource(hash, url, origin)
    }

    private fun fetchText(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w("HTTP ${response.code} while fetching $url")
                    return null
                }
                response.body?.string()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Request failed for $url: ${e.message}")
            null
        }
    }

    private fun downloadPlayerJs(url: String): String? {
        val playerJs = fetchText(url)
        return playerJs?.takeIf { it.isNotEmpty() }
    }
}
