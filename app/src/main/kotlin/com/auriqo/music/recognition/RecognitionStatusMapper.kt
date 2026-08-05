package com.auriqo.music.recognition

import com.music.shazamkit.models.RecognitionStatus

/** Converts provider failures to stable UI states without exposing provider exceptions. */
internal object RecognitionStatusMapper {
    fun fromProviderFailure(message: String?): RecognitionStatus {
        val resolvedMessage = message ?: "Unknown error"
        return if (resolvedMessage.contains("No match", ignoreCase = true)) {
            RecognitionStatus.NoMatch("No matches found. Try again with clearer audio.")
        } else {
            RecognitionStatus.Error(resolvedMessage)
        }
    }

    fun permissionDenied(): RecognitionStatus =
        RecognitionStatus.Error("Microphone permission not granted")
}
