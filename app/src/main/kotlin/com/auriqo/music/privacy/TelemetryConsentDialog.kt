package com.auriqo.music.privacy

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.auriqo.music.R
import kotlinx.coroutines.launch

/** First-launch prompt. A recorded refusal is never shown again unless changed in Settings. */
@Composable
fun TelemetryConsentDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val choice by produceState<TelemetryConsent?>(initialValue = null, context) {
        TelemetryConsentStore.consent(context).collect { value = it }
    }
    val telemetryAvailable = TelemetryConsentStore.isAvailable(context)

    if (choice == null || !TelemetryConsentPolicy.shouldPrompt(telemetryAvailable, choice!!)) return

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.telemetry_consent_title)) },
        text = { Text(stringResource(R.string.telemetry_consent_message)) },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { TelemetryConsentStore.record(context, TelemetryConsent.DECLINED) }
            }) {
                Text(stringResource(R.string.telemetry_consent_decline))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { TelemetryConsentStore.record(context, TelemetryConsent.ACCEPTED) }
            }) {
                Text(stringResource(R.string.telemetry_consent_accept))
            }
        },
    )
}
