package com.auriqo.music.wear.media

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingTest {
    @Test
    fun `playing position is extrapolated from monotonic receive time`() {
        val state =
            NowPlaying(
                isPlaying = true,
                positionMs = 5_000L,
                durationMs = 20_000L,
                playbackSpeed = 1.5f,
                receivedAtElapsedRealtimeMs = 1_000L,
            )

        assertEquals(8_000L, state.positionAt(3_000L))
    }

    @Test
    fun `paused and completed positions remain bounded`() {
        val paused =
            NowPlaying(
                isPlaying = false,
                positionMs = 4_200L,
                durationMs = 10_000L,
                receivedAtElapsedRealtimeMs = 1_000L,
            )
        val completed = paused.copy(isPlaying = true, positionMs = 9_900L)

        assertEquals(4_200L, paused.positionAt(90_000L))
        assertEquals(10_000L, completed.positionAt(90_000L))
    }

    @Test
    fun `same phone session rejects duplicate and out of order snapshots`() {
        val current = NowPlaying(sourceSession = "session-a", sourceSequence = 10L)

        assertEquals(false, shouldApplyIncoming(current, current.copy(sourceSequence = 9L)))
        assertEquals(false, shouldApplyIncoming(current, current.copy(sourceSequence = 10L)))
        assertEquals(true, shouldApplyIncoming(current, current.copy(sourceSequence = 11L)))
    }

    @Test
    fun `new phone session resets ordering without trusting wall time`() {
        val current = NowPlaying(
            sourceSession = "session-a",
            sourceBootCount = 42,
            sourceSessionStartedElapsedMs = 1_000L,
            sourceSequence = 900L,
            sourceUpdatedAtEpochMs = Long.MAX_VALUE,
        )
        val restarted = NowPlaying(
            sourceSession = "session-b",
            sourceBootCount = 42,
            sourceSessionStartedElapsedMs = 2_000L,
            sourceSequence = 1L,
            sourceUpdatedAtEpochMs = 1L,
        )

        assertEquals(true, shouldApplyIncoming(current, restarted))
        assertEquals(false, shouldApplyIncoming(current, NowPlaying(sourceUpdatedAtEpochMs = Long.MAX_VALUE)))
    }

    @Test
    fun `older phone process cannot overwrite a newer session`() {
        val current = NowPlaying(
            sourceSession = "new",
            sourceBootCount = 12,
            sourceSessionStartedElapsedMs = 50_000L,
            sourceSequence = 2L,
        )
        val delayed = current.copy(
            sourceSession = "old",
            sourceSessionStartedElapsedMs = 10_000L,
            sourceSequence = 999L,
            sourceUpdatedAtEpochMs = Long.MAX_VALUE,
        )

        assertEquals(false, shouldApplyIncoming(current, delayed))
    }
}
