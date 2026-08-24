package com.afternote.core.ui.button

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [AfternoteButton] isLoading 계약 가드 (#516, PR #650 리뷰 반영).
 * 단일·dual-action 두 렌더 경로 모두에서 "스피너 표시 + 클릭 차단 + 접근성 이름/상태 유지"가
 * 지켜지는지 검증한다 — dual 경로가 조기 반환으로 계약에서 빠지는 회귀를 막는다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class AfternoteButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `로딩 중 단일 버튼 - 접근성 이름 유지·로딩 상태 노출·클릭 차단`() {
        var clicked = false
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "확인",
                    onClick = { clicked = true },
                    isLoading = true,
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("확인")
            .assertIsNotEnabled()
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "로딩 중"))
            .assertExists()

        composeRule.onNodeWithContentDescription("확인").performClick()
        composeRule.runOnIdle { assertFalse(clicked) }
    }

    @Test
    fun `로딩 중 dual-action - 라벨 대신 스피너·양쪽 클릭 모두 차단`() {
        var primaryClicked = false
        var secondaryClicked = false
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "전체 삭제",
                    onClick = { primaryClicked = true },
                    type = AfternoteButtonType.Variant5,
                    secondaryText = "선택 삭제",
                    onSecondaryClick = { secondaryClicked = true },
                    isLoading = true,
                )
            }
        }

        composeRule.onNodeWithText("전체 삭제").assertDoesNotExist()
        composeRule.onNodeWithText("선택 삭제").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("전체 삭제")
            .assertIsNotEnabled()

        composeRule.onNodeWithContentDescription("전체 삭제").performClick()
        composeRule.runOnIdle {
            assertFalse(primaryClicked)
            assertFalse(secondaryClicked)
        }
    }

    @Test
    fun `로딩 아님 단일 버튼 - 클릭 동작`() {
        var clicked = false
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "확인",
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("확인").performClick()
        composeRule.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun `로딩 아님 dual-action - 좌우 절반이 각자 콜백 호출`() {
        var primaryClicked = false
        var secondaryClicked = false
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "전체 삭제",
                    onClick = { primaryClicked = true },
                    type = AfternoteButtonType.Variant5,
                    secondaryText = "선택 삭제",
                    onSecondaryClick = { secondaryClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("전체 삭제").performClick()
        composeRule.onNodeWithText("선택 삭제").performClick()
        composeRule.runOnIdle {
            assertTrue(primaryClicked)
            assertTrue(secondaryClicked)
        }
    }

    @Test
    fun `보조 라벨을 비활성하면 눌리지 않는다`() {
        // 선택 0개에서 «선택 삭제» 를 막는 자리 (#442). 자체 구현에는 있던 상태라
        // 정본이 담지 않으면 수렴이 그 방어를 지운다.
        var secondaryClicked = false
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "전체 삭제",
                    onClick = {},
                    type = AfternoteButtonType.Variant5,
                    secondaryText = "선택 삭제",
                    onSecondaryClick = { secondaryClicked = true },
                    isSecondaryEnabled = false,
                )
            }
        }

        composeRule.onNodeWithText("선택 삭제").performClick()
        composeRule.runOnIdle { assertFalse(secondaryClicked) }
    }

    @Test
    fun `주 라벨은 보조가 비활성이어도 눌린다`() {
        var primaryClicked = false
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "전체 삭제",
                    onClick = { primaryClicked = true },
                    type = AfternoteButtonType.Variant5,
                    secondaryText = "선택 삭제",
                    onSecondaryClick = {},
                    isSecondaryEnabled = false,
                )
            }
        }

        composeRule.onNodeWithText("전체 삭제").performClick()
        composeRule.runOnIdle { assertTrue(primaryClicked) }
    }

    @Test
    fun `dual-action 좌우 절반이 접근성 트리에 버튼으로 잡힌다`() {
        // 절반씩 독립 클릭인데 Role.Button 이 없으면 스크린리더가 눌 수 있는 요소로 읽지
        // 않는다. mindrecord 자체 구현에는 있고 core 에는 없던 결함이다 (#634).
        composeRule.setContent {
            AfternoteTheme {
                AfternoteButton(
                    text = "전체 삭제",
                    onClick = {},
                    type = AfternoteButtonType.Variant5,
                    secondaryText = "선택 삭제",
                    onSecondaryClick = {},
                )
            }
        }

        listOf("전체 삭제", "선택 삭제").forEach { label ->
            composeRule
                .onNodeWithText(label)
                .assert(
                    SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
                )
        }
    }
}
