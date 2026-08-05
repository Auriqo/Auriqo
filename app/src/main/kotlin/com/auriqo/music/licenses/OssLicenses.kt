package com.auriqo.music.licenses

import android.content.Context

data class OssLicense(
    val name: String,
    val license: String,
    val source: String,
    val variants: String,
)

/** Parses the version-controlled, bundled TSV file; no network is used at runtime. */
object OssLicenses {
    fun load(context: Context): List<OssLicense> = context.assets.open("oss-licenses.tsv")
        .bufferedReader()
        .use { reader -> parse(reader.readLines()) }

    internal fun parse(lines: List<String>): List<OssLicense> = lines.asSequence()
        .filter { it.isNotBlank() && !it.startsWith('#') }
        .mapNotNull { line ->
            val columns = line.split('\t')
            if (columns.size == 5) {
                OssLicense(columns[1], columns[2], columns[3], columns[4])
            } else {
                null
            }
        }
        .toList()
}
