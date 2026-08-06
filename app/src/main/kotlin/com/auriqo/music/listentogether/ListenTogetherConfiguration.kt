package com.auriqo.music.listentogether

import java.net.URI

/** Build-time endpoints; empty values deliberately leave the feature unavailable. */
data class ListenTogetherConfiguration(
    private val configuredServerUrl: String,
    private val configuredShareBaseUrl: String,
) {
    val serverUrl: String? = validServerUrl(configuredServerUrl)
    val shareBaseUrl: String? = validShareBaseUrl(configuredShareBaseUrl)

    val isAvailable: Boolean
        get() = serverUrl != null

    fun inviteLink(roomCode: String): String? = shareBaseUrl
        ?.takeIf { roomCode.isNotBlank() }
        ?.trimEnd('/')
        ?.let { "$it/listen?code=$roomCode" }

    /** A valid stored custom server overrides the deployment-configured endpoint. */
    fun resolveServerUrl(storedServerUrl: String?): String? =
        validServerUrl(storedServerUrl) ?: serverUrl

    companion object {
        fun validServerUrl(value: String?): String? = value.validUrl("wss")

        fun validShareBaseUrl(value: String?): String? = value.validUrl("https")

        private fun String?.validUrl(requiredScheme: String): String? {
            val normalized = this?.trim().orEmpty()
            if (normalized.isEmpty()) return null
            return runCatching { URI(normalized) }
                .getOrNull()
                ?.takeIf { it.scheme.equals(requiredScheme, ignoreCase = true) && !it.host.isNullOrBlank() }
                ?.toString()
        }
    }
}
