package com.afternote.afternote_fe

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.data.PasskeyTestScenario
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PasskeyRegistrationAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var scenario: PasskeyTestScenario

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activityRule.scenario.onActivity { activity ->
            navController =
                TestNavHostController(activity).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            activity.setContent {
                AfternoteTheme {
                    AppNavigation(startDestination = Route.Setting, appState = AppState(navController))
                }
            }
        }
    }

    @Test
    fun passkeyList_queriesServerWithoutLocalFlagAndRefreshesOnReturn() {
        scenario.passkeys = listOf(Passkey(7L, "Server passkey", "2026-09-06T10:00:00"))
        navigate(SettingRoute.PasskeyRoute)
        waitForText("Server passkey")
        composeRule.onNodeWithText("Server passkey").assertIsDisplayed()

        navigate(SettingRoute.PasskeyPasswordRoute)
        scenario.passkeys = listOf(Passkey(8L, "New server passkey", "2026-09-06T11:00:00"))
        composeRule.runOnIdle { navController.popBackStack() }
        waitForText("New server passkey")
        composeRule.onNodeWithText("Server passkey").assertDoesNotExist()
        assertTrue(scenario.listCalls.get() >= 2)
    }

    @Test
    fun passkeyList_failureShowsRetryAndNeverPretendsThereAreNoPasskeys() {
        scenario.listFails = true
        navigate(SettingRoute.PasskeyRoute)
        waitForText("패스키 목록을 불러올 수 없습니다.")
        composeRule.onNodeWithText("패스키 등록").assertDoesNotExist()

        scenario.listFails = false
        composeRule.onNodeWithText("다시 시도").performClick()
        waitForText("패스키 등록")
        composeRule.onNodeWithText("패스키 등록").assertIsDisplayed()
        assertEquals(2, scenario.listCalls.get())
    }

    @Test
    fun passkeyRegistration_withoutCredentialProviderShowsFailureAndDoesNotComplete() {
        // CI의 AOSP lane에서 실제 Credential Manager 실패를 검증한다. 계정이 있는 Play 기기는 대상이 아니다.
        val packageManager = composeRule.activity.packageManager
        val hasPlayServices =
            runCatching { packageManager.getPackageInfo("com.google.android.gms", 0) }.isSuccess
        assumeFalse(hasPlayServices)
        assumeFalse(Build.VERSION.SDK_INT < 28)

        navigate(SettingRoute.PasskeyPasswordRoute)
        listOf("1", "2", "3", "4").forEach { composeRule.onNodeWithText(it).performClick() }
        waitForText("패스키 등록에 실패했습니다. 다시 시도해 주세요.")
        composeRule.onNodeWithText("패스키 등록에 실패했습니다. 다시 시도해 주세요.").assertIsDisplayed()
        assertEquals(1, scenario.optionsCalls.get())
        assertTrue(scenario.registeredCredentials.isEmpty())
        composeRule.onNodeWithText("패스키 생성이 완료되었습니다").assertDoesNotExist()
    }

    private fun navigate(route: Any) {
        composeRule.runOnIdle { navController.navigate(route) }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
