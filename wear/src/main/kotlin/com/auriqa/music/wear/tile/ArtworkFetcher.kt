package com.auriqo.music.wear.tile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ArtworkFetcher {
    private const val MAX_CACHE_ENTRIES = 24
    private const val MAX_IMAGE_DIMENSION = 256

    private val cache = LruCache<String, Bitmap>(MAX_CACHE_ENTRIES)

    @Volatile
    private var cacheDir: File? = null

    fun init(context: android.content.Context) {
        if (cacheDir == null) {
            cacheDir = File(context.cacheDir, "tile_artwork").apply { mkdirs() }
        }
    }

    fun getCached(url: String): Bitmap? = cache.get(url)

    fun fetch(url: String): Bitmap? {
        getCached(url)?.let { return it }

        val dir = cacheDir ?: return null
        val file = File(dir, url.hashCode().toString())

        val fromFile = loadFromFile(file)
        if (fromFile != null) {
            cache.put(url, fromFile)
            return fromFile
        }

        val downloaded = download(url) ?: return null
        saveToFile(downloaded, file)
        cache.put(url, downloaded)
        return downloaded
    }

    private fun download(url: String): Bitmap? =
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = true
            connection.connect()
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { input ->
                BitmapFactory.decodeStream(input)?.let { bitmap ->
                    scaleDown(bitmap)
                }
            }
        }.getOrNull()

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxDimension = maxOf(bitmap.width, bitmap.height)
        if (maxDimension <= MAX_IMAGE_DIMENSION) return bitmap
        val scale = MAX_IMAGE_DIMENSION.toFloat() / maxDimension
        val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun loadFromFile(file: File): Bitmap? =
        if (file.exists()) {
            runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
        } else {
            null
        }

    private fun saveToFile(bitmap: Bitmap, file: File) {
        runCatching {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
            }
        }
    }
}
