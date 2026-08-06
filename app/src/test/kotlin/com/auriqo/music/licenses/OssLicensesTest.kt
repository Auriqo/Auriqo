package com.auriqo.music.licenses

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OssLicensesTest {
    @Test
    fun parser_ignores_comments_and_malformed_rows() {
        val notices = OssLicenses.parse(
            listOf(
                "# header",
                "app\tAuriqo\tGPL-3.0-or-later\thttps://github.com/Auriqo/Auriqo\tall",
                "not-a-license-row",
            ),
        )

        assertEquals(1, notices.size)
        assertEquals("Auriqo", notices.single().name)
    }

    @Test
    fun bundled_attribution_contains_app_and_gms_notices() {
        val asset = sequenceOf(
            File("src/main/assets/oss-licenses.tsv"),
            File("app/src/main/assets/oss-licenses.tsv"),
        ).first { it.isFile }
        val notices = OssLicenses.parse(asset.readLines())
        assertTrue(notices.any { it.name == "Auriqo" && it.license.startsWith("GPL-") })
        assertTrue(notices.any { it.name == "Firebase Analytics" && it.variants == "gms" })
    }
}
