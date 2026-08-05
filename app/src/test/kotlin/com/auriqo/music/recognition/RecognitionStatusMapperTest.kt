package com.auriqo.music.recognition

import com.music.shazamkit.models.RecognitionStatus
import com.music.shazamkit.models.RecognitionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionStatusMapperTest {
    @Test
    fun `permission rejection is a clear error state`() {
        val status = RecognitionStatusMapper.permissionDenied()

        assertTrue(status is RecognitionStatus.Error)
        assertEquals("Microphone permission not granted", (status as RecognitionStatus.Error).message)
    }

    @Test
    fun `no-match failures remain distinguishable from provider errors`() {
        assertTrue(RecognitionStatusMapper.fromProviderFailure("No match returned") is RecognitionStatus.NoMatch)
        val error = RecognitionStatusMapper.fromProviderFailure(null)
        assertTrue(error is RecognitionStatus.Error)
        assertEquals("Unknown error", (error as RecognitionStatus.Error).message)
    }

    @Test
    fun `successful recognition keeps the provider result model intact`() {
        val result = RecognitionResult(
            trackId = "track-id",
            title = "Track",
            artist = "Artist",
            album = null,
            coverArtUrl = null,
            coverArtHqUrl = null,
            genre = null,
            releaseDate = null,
            label = null,
            lyrics = null,
            shazamUrl = null,
            appleMusicUrl = null,
            spotifyUrl = null,
            isrc = null,
        )

        val status = RecognitionStatus.Success(result)
        assertEquals(result, status.result)
    }
}
