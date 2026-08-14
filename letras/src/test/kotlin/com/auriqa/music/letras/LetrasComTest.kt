package com.auriqa.music.letras

import com.auriqa.music.letras.models.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LetrasComTest {
    @Test
    fun `parses only song documents from the local search fixture`() {
        val results = LetrasCom.parseSearchResults(
            javaClass.getResource("/letras/search-response.jsonp")!!.readText(),
        )

        assertEquals(
            listOf(SearchResult("https://www.letras.com/toquinho/49095/", "Aquarela", "Toquinho")),
            results,
        )
    }

    @Test
    fun `preserves lines and verses from the local lyrics fixture`() {
        val lyrics = LetrasCom.parseLyricsFromHtml(
            javaClass.getResource("/letras/lyrics-page.html")!!.readText(),
        )

        assertEquals("Primera línea\nSegunda línea\n\nTercera línea\nCuarta línea", lyrics)
    }

    @Test
    fun `does not turn html formatting whitespace into lyric lines`() {
        val lyrics = LetrasCom.parseLyricsFromHtml(
            """
                <div class="lyric-content">
                  <p>
                    Primera <span>línea</span>
                    <br>
                    Segunda <span>línea</span>
                  </p>
                </div>
            """.trimIndent(),
        )

        assertEquals("Primera línea\nSegunda línea", lyrics)
    }

    @Test
    fun `returns null when the page has no lyric container`() {
        assertNull(LetrasCom.parseLyricsFromHtml("<article><p>Recommendation</p></article>"))
    }

    @Test
    fun `matches source decorations but rejects musical versions and other artists`() {
        val plain = SearchResult("https://www.letras.com/artist/song/", "Song", "Artist")
        val live = SearchResult("https://www.letras.com/artist/song-live/", "Song (Live)", "Artist")

        assertEquals(plain, LetrasCom.findBestMatch(listOf(plain), "Song (Official Video)", "Artist"))
        assertEquals(plain, LetrasCom.findBestMatch(listOf(plain), "Sóng (Lyrics)", "Artist"))
        assertNull(LetrasCom.findBestMatch(listOf(plain), "Song (Live)", "Artist"))
        assertNull(LetrasCom.findBestMatch(listOf(live), "Song", "Artist"))
        assertNull(LetrasCom.findBestMatch(listOf(plain), "Song", "Another Artist"))
    }

    @Test
    fun `accepts a matching primary artist without accepting substring matches`() {
        val result = SearchResult("https://www.letras.com/dj-snake/taki-taki/", "Taki Taki (feat. Selena Gomez)", "DJ Snake")

        assertEquals(result, LetrasCom.findBestMatch(listOf(result), "Taki Taki", "DJ Snake, Selena Gomez"))
        assertNull(LetrasCom.findBestMatch(listOf(result), "Taki Taki", "Snake"))
    }
}
