package com.auriqo.music.lyrics

import com.music.kugou.KuGou
import com.music.lrclib.LrcLib
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LyricsParsingAndFallbackTest {
    @Test
    fun `standard lrc parsing expands timestamps and decodes entities`() {
        val entries = LyricsUtils.parseLyrics("[00:01.20][00:02.30]Rock &amp; Roll")

        assertEquals(listOf(1_200L, 2_300L), entries.map { it.time })
        assertEquals(listOf("Rock & Roll", "Rock & Roll"), entries.map { it.text })
    }

    @Test
    fun `rich sync parsing retains word timing and agent metadata`() {
        val entries = LyricsUtils.parseLyrics("[00:01.00]{agent:v1}<00:01.00>Hello <00:01.50>world")

        assertEquals(1, entries.size)
        assertEquals("Hello world", entries.single().text)
        assertEquals("v1", entries.single().agent)
        assertEquals(2, entries.single().words?.size)
        assertEquals(1.5, entries.single().words?.last()?.startTime ?: 0.0, 0.0001)
    }

    @Test
    fun `fallback policy prefers timed lyrics and otherwise preserves provider order`() {
        val plain = LyricsWithProvider("plain lyrics", "Plain")
        val synced = LyricsWithProvider("[00:01.00]timed lyrics", "Synced")

        assertSame(synced, LyricsFallbackPolicy.select(listOf(plain, synced)))
        assertSame(plain, LyricsFallbackPolicy.select(listOf(plain)))
    }

    @Test
    fun `lyrics cache is bounded and refreshes recently used entries`() {
        val cache = LyricsResultCache(maxEntries = 2)
        cache.put("a", listOf(LyricsResult("A", "a")))
        cache.put("b", listOf(LyricsResult("B", "b")))
        cache.get("a")
        cache.put("c", listOf(LyricsResult("C", "c")))

        assertNull(cache.get("b"))
        assertEquals("a", cache.get("a")?.single()?.lyrics)
        assertEquals("c", cache.get("c")?.single()?.lyrics)
    }

    @Test
    fun `lrclib timestamp parser exposes deterministic sentence timings`() {
        val lyrics = LrcLib.Lyrics("[00:01.23]A long lyric line")

        assertEquals("A long lyric line", lyrics.sentences?.get(1_230L))
    }

    @Test
    fun `kugou search key strips title annotations and normalizes collaborators`() {
        val keyword = KuGou.generateKeyword("Afterglow (Live)", "Ava, Ben & Cora", "Tour")

        assertEquals("Afterglow ", keyword.title)
        assertEquals("Ava\u3001Ben\u3001Cora", keyword.artist)
        assertFalse(keyword.artist.contains(","))
        assertFalse(keyword.artist.contains("&"))
        assertEquals("Tour", keyword.album)
    }
}
