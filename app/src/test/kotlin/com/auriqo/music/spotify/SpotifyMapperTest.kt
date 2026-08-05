package com.auriqo.music.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyMapperTest {
    @Test
    fun `matching ignores featuring and remaster metadata`() {
        val score = SpotifyMapper.matchScore(
            spotifyTitle = "Midnight City (feat. Guest) [2011 Remaster]",
            spotifyArtist = "M83",
            spotifyDurationMs = 244_000,
            candidateTitle = "Midnight City",
            candidateArtist = "M83",
            candidateDurationSec = 244,
        )

        assertTrue(score >= SpotifyMapper.earlyExitThreshold())
    }

    @Test
    fun `precomputed matching is equivalent to direct matching`() {
        val precomputed = SpotifyMapper.precompute("Teardrop", "Massive Attack", 331_000)
        val direct = SpotifyMapper.matchScore(
            "Teardrop", "Massive Attack", 331_000,
            "Teardrop (Remastered)", "Massive Attack", 331,
        )

        assertEquals(
            direct,
            SpotifyMapper.matchScorePrecomputed(
                precomputed,
                "Teardrop (Remastered)",
                "Massive Attack",
                331,
            ),
            0.000001,
        )
    }
}
