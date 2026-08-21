package com.auriqo.music.utils.cipher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerJsUrlParserTest {
    @Test
    fun iframeApi_parsesEscapedWidgetApiPath() {
        val iframeApi =
            """var scriptUrl = 'https:\/\/www.youtube.com\/s\/player\/2574220e\/www-widgetapi.vflset\/www-widgetapi.js';"""

        assertEquals(
            "https://www.youtube.com/s/player/2574220e/player_ias.vflset/en_GB/base.js",
            PlayerJsUrlParser.fromIframeApi(iframeApi),
        )
    }

    @Test
    fun embedPage_parsesPlayerEmbedJsUrl() {
        val embedPage =
            """{"jsUrl":"/s/player/2574220e/player_embed.vflset/es_MX/base.js"}"""

        assertEquals(
            "https://www.youtube.com/s/player/2574220e/player_embed.vflset/es_MX/base.js",
            PlayerJsUrlParser.fromEmbedPage(embedPage),
        )
    }

    @Test
    fun unrelatedHtml_isRejected() {
        assertNull(PlayerJsUrlParser.fromIframeApi("var scriptUrl = '/s/player/not-a-hash/base.js'"))
        assertNull(PlayerJsUrlParser.fromEmbedPage("{\"jsUrl\":\"/assets/player.js\"}"))
    }
}
