package com.afternote.feature.home.presentation.receiver

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.PROFILE_ICON_TEST_TAG
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 이 테스트의 관심 밖인 외부 라우팅을 채우는 no-op 묶음. */
private val noopActions =
    ReceiverHomeActions(
        onNavigateToMindRecord = {},
        onNavigateToTimeLetter = {},
        onNavigateToAfternote = {},
    )

/**
 * 수신자 홈 헤더가 수신자가 실제로 할 수 있는 액션만 두는지 (#613, 시안 4327:73626).
 *
 * 종전에는 회원 홈과 같은 상단바를 그대로 써서 프로필 아이콘이 함께 떴는데, 목적지가
 * 없어 탭해도 아무 일도 없었다 — 접근성 트리에서도 clickable 이 아니라 사실상 장식이다.
 * 시안의 수신자 홈에도 톱니 하나뿐이라 그 상태를 고정한다.
 *
 * 설정 아이콘의 이름도 함께 고정한다. 종전에는 둘 다 `contentDescription = null` 이라
 * 스크린리더가 **유일하게 눌리는 액션**조차 읽어 주지 못했다. 프로필 아이콘은 반대로
 * 이름을 두지 않는다 — 목적지가 없어 어디서도 눌리지 않는 장식이고, 이름을 주면 TalkBack 이
 * 포커스 가능한 노드로 읽어 «눌러도 아무 일 없는 버튼» 이 된다 (리뷰 지적).
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
                    actions = noopActions,
                )
            }
        }

        // 프로필 아이콘은 목적지가 없는 장식이라 semantics 이름이 없다 — 테스트 태그로 본다.
        composeRule.onNodeWithTag(PROFILE_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `설정 아이콘을 그리지 않는다`() {
        // 톱니는 **회원 설정 화면**을 그대로 열었고, 그 화면의 유일한 항목이 「로그아웃」이다.
        // 수신자는 로그인한 적이 없는 사용자(X-Auth-Code 기반)라 지울 세션이 없다 (#613).
        //
        // 「이름이 있고 눌린다」를 단언하던 자리를 뒤집는다 — 진입점 자체가 없어야 한다.
        composeRule.setContent {
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState = ReceiverHomeUiState.Loading,
                    onEvent = {},
                    actions = noopActions,
                )
            }
        }

        val setting = composeRule.activity.getString(R.string.core_ui_home_top_bar_setting)
        composeRule.onNodeWithContentDescription(setting).assertDoesNotExist()
    }

    @Test
    fun `헤더에 누를 수 있는 액션이 하나도 없다`() {
        // 개별 아이콘을 이름으로 확인하는 것과 별개로, 「헤더에 회원 액션이 새로 생기지 않는다」를
        // 통째로 고정한다 — 새 아이콘이 추가되면 이름을 모르니 위 두 단언은 그대로 통과한다.
        composeRule.setContent {
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState = ReceiverHomeUiState.Loading,
                    onEvent = {},
                    actions = noopActions,
                )
            }
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }
}
