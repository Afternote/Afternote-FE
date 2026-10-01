package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.navigation.SettingExternalActions
import com.afternote.feature.setting.presentation.navigation.SettingNavHost
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/** #1438: 실제 SettingNavHost의 entry 이탈이 보류 중 저장을 취소하고 새 entry를 만드는지 검증한다. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProfileSaveBackAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var profileRepository: MyProfileRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun systemBackDuringPendingSaveCancelsRequestAndReentryLoadsServerProfile() {
        // 기존 app Hilt fixture가 제공하는 동일 저장소의 응답만 보류한다. 프로덕션 nav/VM은 그대로다.
        val repository = profileRepository as FakeUserRepository
        val saved = User("서버 이름", "qa@example.test", "01000000000", null)
        repository.profile = saved
        val pendingSave = CompletableDeferred<User>()
        val cancelled = CompletableDeferred<Unit>()
        var exits = 0
        repository.onUpdateMyProfile = { _, _, _ ->
            try {
                pendingSave.await()
            } catch (error: CancellationException) {
                cancelled.complete(Unit)
                throw error
            }
        }
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AfternoteTheme {
                    SettingNavHost(
                        navigationCallbacks = FeatureNavigationCallbacks { exits += 1 },
                        externalActions =
                            object : SettingExternalActions {
                                override fun onLogoutSuccess() = Unit

                                override fun onWithdrawSuccess() = Unit
                            },
                    )
                }
            }
        }
        waitForText("프로필 수정")
        composeRule.onNodeWithText("프로필 수정").performClick()
        waitForText("프로필 설정")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("저장 중 임시 이름")
        composeRule.onNodeWithText("수정하기").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { repository.profileUpdateCalls.size == 1 }
        composeRule.onNodeWithText("수정하기").assertIsNotEnabled()
        assertFalse(pendingSave.isCompleted)

        // Activity의 실제 back dispatcher가 SettingNavHost/NavDisplay의 back 처리를 실행한다.
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("프로필 수정")
        composeRule.waitUntil(10_000) { cancelled.isCompleted }
        assertEquals(0, exits)
        assertEquals(saved, repository.profile)

        val loadsBeforeReentry = repository.profileCalls
        composeRule.onNodeWithText("프로필 수정").performClick()
        waitForText("서버 이름")
        composeRule.waitUntil(10_000) { repository.profileCalls > loadsBeforeReentry }
        composeRule.onNodeWithText("수정하기").performScrollTo().assertIsEnabled()

        // 취소 뒤 늦은 성공도 현재 entry를 닫거나 서버 fake의 정본을 바꾸지 않는다.
        pendingSave.complete(saved.copy(name = "저장 중 임시 이름"))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("프로필 설정").assertIsDisplayed()
        assertEquals(saved, repository.profile)
        assertEquals(1, repository.profileUpdateCalls.size)
        assertTrue(cancelled.isCompleted)
        assertEquals(0, exits)
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
    }
}
