package com.afternote.feature.mindrecord.presentation.component

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 늦게 도착한 본문이 에디터에 실리는지 (#1018).
 *
 * 종전에는 첫 컴포지션 1회만 시드해서, «화면이 먼저 뜨고 값이 나중에 오는» 이어쓰기·수정
 * 프리필에서 본문이 비어 있었다. 호출부 둘이 각자 `key(draftLoaded)` 로 컴포넌트를 통째로
 * 재생성해 우회했는데, 재마운트는 진행 중인 업로드 스코프·피커 콜백·IME 조합 상태를 함께
 * 날린다. **이 테스트는 key() 없이** 값이 실리는 것을 고정한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WriteTextFieldReseedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `나중에 도착한 본문이 에디터에 실린다`() {
        var content by mutableStateOf<String?>(null)
        composeRule.setContent {
            AfternoteTheme {
                WriteTextField(
                    value = content,
                    onDraftCountClick = {},
                    onSaveDraftClick = {},
                )
            }
        }

        composeRule.runOnIdle { content = "<p>서버에서 도착한 임시저장 본문</p>" }

        composeRule.onNodeWithText("서버에서 도착한 임시저장 본문").assertIsDisplayed()
    }

    @Test
    fun `되돌아온 같은 값은 다시 쓰지 않는다`() {
        // onValueChange 로 올라간 HTML 이 그대로 되돌아오는 것이 정상 경로다. 그때마다
        // setHtml 을 부르면 커서가 튄다 — 값이 같으면 아무 일도 하지 않아야 한다.
        var content by mutableStateOf<String?>("<p>이미 쓴 본문</p>")
        var changes = 0
        composeRule.setContent {
            AfternoteTheme {
                WriteTextField(
                    value = content,
                    onValueChange = { changes += 1 },
                    onDraftCountClick = {},
                    onSaveDraftClick = {},
                )
            }
        }
        composeRule.waitForIdle()
        val afterFirstSeed = changes

        composeRule.runOnIdle { content = "<p>이미 쓴 본문</p>" }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("이미 쓴 본문").assertIsDisplayed()
        // 같은 값이 되돌아온 뒤에도 에디터 상태가 다시 바뀌지 않는다.
        org.junit.Assert.assertEquals(afterFirstSeed, changes)
    }
}
