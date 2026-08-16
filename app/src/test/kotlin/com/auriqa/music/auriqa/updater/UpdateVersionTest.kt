package com.auriqo.music.echomusic.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test
    fun semanticVersionsCompareWithPrereleaseSupport() {
        assertTrue(isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(isNewerVersion("v1.0.3", "1.0.3-alpha.2"))
        assertTrue(isNewerVersion("1.0.0", "1.0.0-beta"))
        assertFalse(isNewerVersion("1.0.0-beta", "1.0.0"))
        assertFalse(isNewerVersion("1.0.0", "1.0.0"))
    }

    @Test
    fun assetSelectionMatchesInstalledVariantAndBuildType() {
        val assets = listOf(
            UpdateAsset("app-universal-foss-debug.apk", "", 0L, null),
            UpdateAsset("app-universal-foss-release.apk", "", 0L, null),
            UpdateAsset("app-universal-gms-debug.apk", "", 0L, null),
            UpdateAsset("app-universal-gms-release.apk", "", 0L, null),
            UpdateAsset("notes.txt", "", 0L, null),
        )

        assertEquals(
            "app-universal-foss-debug.apk",
            selectUpdateAsset(assets, variant = "foss", debug = true)?.name,
        )
        assertEquals(
            "app-universal-gms-release.apk",
            selectUpdateAsset(assets, variant = "gms", debug = false)?.name,
        )
        assertNull(selectUpdateAsset(assets, variant = "tv", debug = false))
    }
}
