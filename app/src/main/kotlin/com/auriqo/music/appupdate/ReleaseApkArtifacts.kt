package com.auriqo.music.appupdate

/** A release asset advertised by the GitHub release API. */
data class ReleaseApkAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

/**
 * Canonical names supported for FOSS release APKs.
 *
 * The current release workflow publishes the universal APK. ABI-specific names remain
 * supported for compatibility with historical or mirrored release assets; native libraries
 * make selecting an arbitrary first APK unsafe. When an ABI-specific asset is unavailable,
 * the universal APK is the compatible fallback.
 */
object ReleaseApkArtifacts {
    private const val RELEASE_DOWNLOAD_BASE =
        "https://github.com/Auriqo/Auriqo/releases/download"

    fun canonicalArchitecture(architecture: String): String = when (architecture) {
        "arm64", "armeabi", "x86", "x86_64", "universal" -> architecture
        else -> "universal"
    }

    fun assetNameFor(architecture: String): String =
        "auriqo-foss-${canonicalArchitecture(architecture)}.apk"

    fun downloadUrl(versionTag: String, architecture: String): String =
        "$RELEASE_DOWNLOAD_BASE/$versionTag/${assetNameFor(architecture)}"

    fun selectCompatibleAsset(
        assets: Iterable<ReleaseApkAsset>,
        architecture: String,
    ): ReleaseApkAsset? {
        val byName = assets.associateBy { it.name.lowercase() }
        val requested = assetNameFor(architecture).lowercase()
        return byName[requested] ?: byName[assetNameFor("universal").lowercase()]
    }
}
