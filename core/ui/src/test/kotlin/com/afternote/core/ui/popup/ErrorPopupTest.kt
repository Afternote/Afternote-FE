package com.afternote.core.ui.popup

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 공통 오류 팝업 3종(시안 `3628:23827`)의 문구·액션 계약 가드 (#446).
 *
 * 문구를 테스트 안에 다시 적지 않고 리소스에서 읽는 이유 — 그렇게 하면 «리소스가 이 팝업에
 * 연결돼 있는가» 가 아니라 «내가 적은 문자열이 화면에 있는가» 를 검증하게 되어, 팝업이 남의
 * 문자열을 가리키도록 잘못 배선돼도 통과한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ErrorPopupTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `네트워크 연결 오류 팝업은 지정된 문구와 재시도 액션을 낸다`() {
        var retried = 0
        composeRule.setContent {
            AfternoteTheme {
                NetworkErrorPopup(onRetry = { retried += 1 }, onDismiss = {})
            }
        }

        assertPopupTexts(
            titleRes = R.string.core_ui_network_error_title,
            descriptionRes = R.string.core_ui_network_error_description,
        )
        clickButton(R.string.core_ui_network_error_retry)

        composeRule.runOnIdle { assertEquals(1, retried) }
    }

    @Test
    fun `서버 오류 팝업은 지정된 문구와 재시도 액션을 낸다`() {
        var retried = 0
        composeRule.setContent {
            AfternoteTheme {
                ServerErrorPopup(onRetry = { retried += 1 }, onDismiss = {})
            }
        }

        assertPopupTexts(
            titleRes = R.string.core_ui_server_error_title,
            descriptionRes = R.string.core_ui_server_error_description,
        )
        clickButton(R.string.core_ui_server_error_retry)

        composeRule.runOnIdle { assertEquals(1, retried) }
    }

    @Test
    fun `업로드 실패 팝업은 지정된 문구와 재시도 액션을 낸다`() {
        var retried = 0
        composeRule.setContent {
            AfternoteTheme {
                UploadErrorPopup(onRetry = { retried += 1 }, onDismiss = {})
            }
        }

        assertPopupTexts(
            titleRes = R.string.core_ui_upload_error_title,
            descriptionRes = R.string.core_ui_upload_error_description,
        )
        clickButton(R.string.core_ui_upload_error_retry)

        composeRule.runOnIdle { assertEquals(1, retried) }
    }

    private fun assertPopupTexts(
        titleRes: Int,
        descriptionRes: Int,
    ) {
        composeRule.onNodeWithText(stringResourceValue(titleRes)).assertExists()
        composeRule.onNodeWithText(stringResourceValue(descriptionRes)).assertExists()
    }

    private fun clickButton(textRes: Int) {
        composeRule.onNodeWithText(stringResourceValue(textRes)).performClick()
    }

    private fun stringResourceValue(resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)
}
