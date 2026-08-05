package com.auriqo.music.ui

import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.auriqo.music.MainActivity
import com.auriqo.music.R
import com.auriqo.music.ui.player.PlayerTransportButton
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Exercises real MainActivity navigation. The activity rule receives its smoke
 * intent before [MainActivity.onStart], so transient prompts cannot block tests.
 */
class FeatureSurfaceSmokeTest {
    private val composeRule = createEmptyComposeRule()
    private val activityRule = ActivityScenarioRule<MainActivity>(smokeActivityIntent())

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(composeRule).around(activityRule)

    @Test
    fun home_navigation_opens_the_real_search_screen() {
        composeRule.onNodeWithTag("home.surface").assertIsDisplayed()

        waitForSurface("navigation.search_input")
        composeRule.onNodeWithTag("navigation.search_input").performClick()
        waitForSurface("search.surface")
        composeRule.onNodeWithTag("search.surface").assertIsDisplayed()
    }

    @Test
    fun search_accepts_text_in_the_real_search_field() {
        waitForSurface("navigation.search_input")
        composeRule.onNodeWithTag("navigation.search_input").performClick()
        waitForSurface("search.surface")

        composeRule.onNodeWithTag("search.query")
            .performClick()
            .performTextInput("Midnight")
        composeRule.onNodeWithTag("search.query").assertTextEquals("Midnight")
    }

    @Test
    fun settings_filters_and_navigates_to_player_settings() {
        composeRule.onNodeWithTag("home.settings").performClick()
        waitForSurface("settings.dialog.open")
        composeRule.onNodeWithTag("settings.dialog.open").performClick()
        waitForSurface("settings.surface")

        composeRule.onNodeWithTag("settings.filter").performTextInput("player")
        composeRule.onNodeWithTag("settings.player").assertIsDisplayed().performClick()
        waitForSurface("settings.player.surface")
        composeRule.onNodeWithTag("settings.player.surface").assertIsDisplayed()
    }

    private fun waitForSurface(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

/** The production transport component is shared by the full player controls. */
class PlayerTransportButtonSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun player_transport_button_updates_its_real_play_pause_semantics() {
        composeRule.setContent {
            var isPlaying by remember { mutableStateOf(false) }
            MaterialTheme {
                PlayerTransportButton(
                    isPlaying = isPlaying,
                    isListenTogetherGuest = false,
                    isMuted = false,
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier,
                )
            }
        }

        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithTag("player.transport")
            .assertContentDescriptionEquals(targetContext.getString(R.string.play))
        composeRule.onNodeWithTag("player.transport").performClick()
        composeRule.onNodeWithTag("player.transport")
            .assertContentDescriptionEquals(targetContext.getString(R.string.pause))
    }
}

private fun smokeActivityIntent(): Intent = Intent(
    InstrumentationRegistry.getInstrumentation().targetContext,
    MainActivity::class.java,
).apply {
    putExtra(MainActivity.EXTRA_UI_SMOKE_TEST, true)
    putExtra(MainActivity.EXTRA_UI_SMOKE_START_DESTINATION, "home")
}
