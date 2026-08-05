package com.dpi

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.auriqo.music.constants.DensityScaleKey
import org.junit.Assert.assertEquals
import org.junit.Test

class DensityScalerTest {
    @Test
    fun `a migrated density value is used at the next startup`() {
        val migratedPreferences = mutablePreferencesOf().apply {
            this[DensityScaleKey] = 1.15f
        }

        assertEquals(1.15f, startupDensityScale(migratedPreferences), 0f)
    }

    @Test
    fun `a fresh install starts at the default density`() {
        assertEquals(1f, startupDensityScale(mutablePreferencesOf()), 0f)
    }
}
