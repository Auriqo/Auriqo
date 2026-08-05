package com.auriqo.music.appupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseApkArtifactsTest {
    private val universal = ReleaseApkAsset(
        name = "auriqo-foss-universal.apk",
        downloadUrl = "https://example.test/universal",
        sizeBytes = 10,
    )
    private val arm64 = ReleaseApkAsset(
        name = "auriqo-foss-arm64.apk",
        downloadUrl = "https://example.test/arm64",
        sizeBytes = 20,
    )

    @Test
    fun `selects the current ABI even when another APK appears first`() {
        val selected = ReleaseApkArtifacts.selectCompatibleAsset(
            assets = listOf(universal, arm64),
            architecture = "arm64",
        )

        assertEquals(arm64, selected)
    }

    @Test
    fun `uses universal only as the deterministic compatible fallback`() {
        assertEquals(
            universal,
            ReleaseApkArtifacts.selectCompatibleAsset(listOf(universal), "x86_64"),
        )
    }

    @Test
    fun `does not select an incompatible APK without a universal fallback`() {
        assertNull(
            ReleaseApkArtifacts.selectCompatibleAsset(listOf(arm64), "x86"),
        )
    }

    @Test
    fun `uses canonical release asset names and URLs`() {
        assertEquals("auriqo-foss-x86_64.apk", ReleaseApkArtifacts.assetNameFor("x86_64"))
        assertEquals(
            "https://github.com/Auriqo/Auriqo/releases/download/v1.0.0/auriqo-foss-arm64.apk",
            ReleaseApkArtifacts.downloadUrl("v1.0.0", "arm64"),
        )
    }
}
