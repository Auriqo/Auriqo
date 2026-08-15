package com.auriqo.music.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.auriqo.music.wear.media.AuriqoMediaSessionService
import com.auriqo.music.wear.media.PhoneSyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhoneSyncManager.ensureConnected(this)
        AuriqoMediaSessionService.start(this)

        setContent {
            MaterialTheme(
                colors = AuriqoWearColors.themeColors,
            ) {
                Scaffold(
                    timeText = { TimeText() },
                ) {
                    NowPlayingScreen(Modifier.fillMaxSize())
                }
            }
        }
    }
}
