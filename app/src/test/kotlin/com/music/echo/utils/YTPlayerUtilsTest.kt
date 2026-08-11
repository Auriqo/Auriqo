package iad1tya.echo.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YTPlayerUtilsTest {
    @Test
    fun nTransformRunsOnlyForRawPlayerUrlsWithAnNParameter() {
        val rawUrl = "https://cdn.example/audio?n=raw-value"

        assertTrue(
            YTPlayerUtils.shouldApplyNTransform(
                YTPlayerUtils.StreamUrlSource.RawPlayer,
                rawUrl,
            ),
        )
        assertFalse(
            YTPlayerUtils.shouldApplyNTransform(
                YTPlayerUtils.StreamUrlSource.NewPipe,
                rawUrl,
            ),
        )
        assertFalse(
            YTPlayerUtils.shouldApplyNTransform(
                YTPlayerUtils.StreamUrlSource.RawPlayer,
                "https://cdn.example/audio?itag=251",
            ),
        )
    }
}
