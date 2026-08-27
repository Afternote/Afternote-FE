package com.afternote.afternote_fe

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.navigation.rememberAfternoteAppState
import com.afternote.afternote_fe.navigation.rememberMindRecordNavActions
import com.afternote.afternote_fe.navigation.rememberSettingNavActions
import com.afternote.afternote_fe.navigation.rememberTimeLetterNavActions
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordNavActions
import com.afternote.feature.setting.presentation.navigation.SettingNavActions
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterNavActions
import org.junit.Rule
import org.junit.Test

class AppNavigationActionsBackAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun featurePopBackActions_eachReturnToPreviousDestination() {
        var appState: AppState? = null
        var mindRecordActions: MindRecordNavActions? = null
        var timeLetterActions: TimeLetterNavActions? = null
        var settingActions: SettingNavActions? = null

        composeRule.setContent {
            val currentAppState = rememberAfternoteAppState()
            val currentMindRecordActions = rememberMindRecordNavActions(currentAppState.navController)
            val currentTimeLetterActions = rememberTimeLetterNavActions(currentAppState.navController)
            val currentSettingActions = rememberSettingNavActions(currentAppState)
            SideEffect {
                appState = currentAppState
                mindRecordActions = currentMindRecordActions
                timeLetterActions = currentTimeLetterActions
                settingActions = currentSettingActions
            }
            AfternoteTheme {
                NavHost(
                    navController = currentAppState.navController,
                    startDestination = Route.Home,
                ) {
                    composable<Route.Home> { Text(HOME_ROUTE) }
                    composable<Route.MindRecord> { Text(MIND_RECORD_ROUTE) }
                    composable<Route.TimeLetter> { Text(TIME_LETTER_ROUTE) }
                    composable<Route.Setting> { Text(SETTING_ROUTE) }
                }
            }
        }

        val currentAppState = checkNotNull(appState)
        composeRule.onNodeWithText(HOME_ROUTE).assertIsDisplayed()

        composeRule.runOnIdle { currentAppState.navController.navigate(Route.MindRecord) }
        composeRule.onNodeWithText(MIND_RECORD_ROUTE).assertIsDisplayed()
        composeRule.runOnIdle { checkNotNull(mindRecordActions).popBack() }
        composeRule.onNodeWithText(HOME_ROUTE).assertIsDisplayed()

        composeRule.runOnIdle { currentAppState.navController.navigate(Route.TimeLetter) }
        composeRule.onNodeWithText(TIME_LETTER_ROUTE).assertIsDisplayed()
        composeRule.runOnIdle { checkNotNull(timeLetterActions).popBack() }
        composeRule.onNodeWithText(HOME_ROUTE).assertIsDisplayed()

        composeRule.runOnIdle { currentAppState.navController.navigate(Route.Setting) }
        composeRule.onNodeWithText(SETTING_ROUTE).assertIsDisplayed()
        composeRule.runOnIdle { checkNotNull(settingActions).popBack() }
        composeRule.onNodeWithText(HOME_ROUTE).assertIsDisplayed()
    }

    private companion object {
        const val HOME_ROUTE = "home route"
        const val MIND_RECORD_ROUTE = "mind record route"
        const val TIME_LETTER_ROUTE = "time letter route"
        const val SETTING_ROUTE = "setting route"
    }
}
