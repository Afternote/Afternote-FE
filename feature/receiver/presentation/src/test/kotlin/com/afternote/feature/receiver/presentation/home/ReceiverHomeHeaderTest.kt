package com.afternote.feature.receiver.presentation.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.home.model.ReceiverHomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 수신자 홈 헤더가 수신자가 실제로 할 수 있는 액션만 두는지 (#613, 시안 4327:73626).
 *
 * 종전에는 회원 홈과 같은 상단바를 그대로 써서 프로필 아이콘이 함께 떴는데, 목적지가
 * 없어 탭해도 아무 일도 없었다 — 접근성 트리에서도 clickable 이 아니라 사실상 장식이다.
 * 시안의 수신자 홈에도 톱니 하나뿐이라 그 상태를 고정한다.
 *
 * 아이콘 이름도 함께 고정한다. 둘 다 `contentDescription = null` 이라 스크린리더가
 * 유일하게 눌리는 액션조차 읽어 주지 못했다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverHomeHeaderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `수신자 홈에는 프로필 아이콘이 없다`() {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState = ReceiverHomeUiState.Loading,
                    onEvent = {},
                    actions = ReceiverHomeActions(),
                )
            }
        }

        val profile = composeRule.activity.getString(R.string.core_ui_home_top_bar_profile)
        composeRule.onNodeWithContentDescription(profile).assertDoesNotExist()
    }

    @Test
    fun `설정 아이콘은 이름이 있고 눌린다`() {
        var settingClicks = 0
        composeRule.setContent {
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState = ReceiverHomeUiState.Loading,
                    onEvent = {},
                    actions = ReceiverHomeActions(onSettingClick = { settingClicks += 1 }),
                )
            }
        }

        val setting = composeRule.activity.getString(R.string.core_ui_home_top_bar_setting)
        composeRule.onNodeWithContentDescription(setting).assertIsDisplayed().assertHasClickAction()
    }
}
