package com.auriqo.music.eq

import com.auriqo.music.eq.data.ParametricEQ
import com.auriqo.music.eq.data.ParametricEQBand
import com.auriqo.music.eq.data.ParametricEQParser
import com.auriqo.music.eq.data.FilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricEQParserTest {
    @Test
    fun `equalizer preset round trips through standard file format`() {
        val profile = ParametricEQ(
            preamp = -3.5,
            bands = listOf(ParametricEQBand(1_000.0, 2.5, 1.2, FilterType.PK)),
        )

        val parsed = ParametricEQParser.parseText(ParametricEQParser.toFileFormat(profile))

        assertEquals(-3.5, parsed.preamp, 0.0001)
        assertEquals(profile.bands.single().frequency, parsed.bands.single().frequency, 0.0001)
        assertEquals(profile.bands.single().gain, parsed.bands.single().gain, 0.0001)
    }

    @Test
    fun `validation rejects unsafe band values and excessive presets`() {
        val invalid = ParametricEQ(
            preamp = 60.0,
            bands = List(ParametricEQ.MAX_BANDS + 1) { ParametricEQBand(0.0, 40.0, 0.0) },
        )

        val errors = ParametricEQParser.validate(invalid)
        assertTrue(errors.any { it.contains("Preamp") })
        assertTrue(errors.any { it.contains("maximum") })
        assertTrue(errors.any { it.contains("Frequency") })
        assertTrue(errors.any { it.contains("Gain") })
        assertTrue(errors.any { it.contains("Q factor") })
    }
}
