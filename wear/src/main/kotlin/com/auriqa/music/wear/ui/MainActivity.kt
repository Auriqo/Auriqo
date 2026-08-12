package com.auriqo.music.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.auriqo.music.wear.media.MediaBrowserManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MediaBrowserManager.ensureConnected(this)

        setContent {
            MaterialTheme(
                colors = AuriqoWearColors.themeColors,
            ) {
                Scaffold(
                    timeText = { TimeText() },
                ) {
                    val listState = rememberScalingLazyListState()
                    ScalingLazyColumn(
                        state = listState,
                    ) {
                        item {
                            NowPlayingScreen()
                        }
                    }
                }
            }
        }
    }
}
