package com.afternote.feature.receiver.presentation.error

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.afternote.core.ui.R as CoreUiR

/**
 * [ReceiverErrorPopupHost] 의 갈래 → 팝업 대응 가드 (#446).
 *
 * 갈래를 잘못 이어도(예: 서버 오류에 업로드 실패 팝업) 컴파일은 통과하고 화면도 팝업 하나를
 * 정상적으로 띄운다 — 그 조용한 오배선을 잡는 것이 이 테스트다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverErrorPopupHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun networkPopup_showsNetworkCopy() {
        assertPopupShows(ReceiverErrorPopup.NETWORK, CoreUiR.string.core_ui_network_error_title)
    }

    @Test
    fun serverPopup_showsServerCopy() {
        assertPopupShows(ReceiverErrorPopup.SERVER, CoreUiR.string.core_ui_server_error_title)
    }

    @Test
    fun uploadPopup_showsUploadCopy() {
        assertPopupShows(ReceiverErrorPopup.UPLOAD, CoreUiR.string.core_ui_upload_error_title)
    }

    @Test
    fun nullPopup_showsNothing() {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverErrorPopupHost(popup = null, onRetry = {}, onDismiss = {})
            }
        }

        composeRule
            .onNodeWithText(stringResourceValue(CoreUiR.string.core_ui_server_error_title))
            .assertDoesNotExist()
    }

    private fun assertPopupShows(
        popup: ReceiverErrorPopup,
        titleRes: Int,
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverErrorPopupHost(popup = popup, onRetry = {}, onDismiss = {})
            }
        }

        composeRule.onNodeWithText(stringResourceValue(titleRes)).assertExists()
    }

    private fun stringResourceValue(resourceId: Int): String = RuntimeEnvironment.getApplication().getString(resourceId)
}
