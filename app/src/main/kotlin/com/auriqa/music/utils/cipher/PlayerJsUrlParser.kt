package com.auriqo.music.utils.cipher

/** Extracts the base player JavaScript URL from YouTube's iframe and embed resources. */
internal object PlayerJsUrlParser {
    private const val YOUTUBE_BASE_URL = "https://www.youtube.com"
    private const val PLAYER_JS_URL_TEMPLATE =
        "$YOUTUBE_BASE_URL/s/player/%s/player_ias.vflset/en_GB/base.js"

    private val PLAYER_HASH_PATH = Regex("""/s/player/([A-Za-z0-9_-]{8})/""")
    private val EMBED_PLAYER_URL = Regex(
        """[\"'](/s/player/[A-Za-z0-9_-]+/player_(?:ias|embed)\.vflset/[^\"']+/base\.js)[\"']""",
    )

    fun fromIframeApi(body: String): String? {
        val hash = PLAYER_HASH_PATH.find(unescape(body))?.groupValues?.get(1) ?: return null
        return PLAYER_JS_URL_TEMPLATE.format(hash)
    }

    fun fromEmbedPage(body: String): String? {
        val normalized = unescape(body)
        val relativeUrl = EMBED_PLAYER_URL.find(normalized)?.groupValues?.get(1) ?: return null
        return "$YOUTUBE_BASE_URL$relativeUrl"
    }

    private fun unescape(body: String): String = body
        .replace("\\/", "/")
        .replace("\\u002F", "/", ignoreCase = true)
}
