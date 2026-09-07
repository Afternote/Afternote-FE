package com.afternote.feature.home.presentation.receiver

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadErrorPopup
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadState
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.afternote.core.ui.R as CoreUiR

/** 이 테스트의 관심 밖인 외부 라우팅을 채우는 no-op 묶음. */
private val noopActions =
    ReceiverHomeActions(
        onNavigateToMindRecord = {},
        onNavigateToTimeLetter = {},
        onNavigateToAfternote = {},
    )

/**
 * 수신자 홈 내려받기 실패 안내가 **어디로 나가는지** (#1391 → #446 · #1737).
 *
 * 종전 Toast 는 Compose semantics 에 잡히지 않아 어떤 테스트로도 단언할 수 없었고(#1391 이
 * Snackbar 로 옮겼다), 지금은 재시도로 풀릴 수 있는 실패가 공통 오류 팝업으로 나간다.
 * **둘의 갈림이 이 파일의 계약이다** — 재시도해도 같은 실패(미구현 내보내기)만 Snackbar 로 남는다.
 *
 * 문구는 리소스 원문으로 대조한다 (모듈 테스트 관례).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverHomeDownloadSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `내려받기 서버 실패는 서버 오류 팝업으로 안내한다`() {
        setHomeContent(ReceiverDownloadState.FailedWithRetry(ReceiverDownloadErrorPopup.SERVER))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_server_error_title)).assertIsDisplayed()
    }

    @Test
    fun `연결 실패는 네트워크 오류 팝업으로 안내한다`() {
        setHomeContent(ReceiverDownloadState.FailedWithRetry(ReceiverDownloadErrorPopup.NETWORK))

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_network_error_title)).assertIsDisplayed()
    }

    /**
     * 저장 실패는 시안 4종에 대응 항목이 없다. 업로드 팝업을 돌려쓰면 「파일 업로드에
     * 실패했습니다」 가 뜨는데 이 경로는 내려받은 파일을 저장하는 중이라 사실과 다르다.
     */
    @Test
    fun `파일 저장 실패는 저장 실패 문구로 안내한다`() {
        setHomeContent(ReceiverDownloadState.FailedWithRetry(ReceiverDownloadErrorPopup.SAVE))

        composeRule.onNodeWithText(string(R.string.home_receiver_download_save_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.home_receiver_download_all_save_failed)).assertIsDisplayed()
        assertEquals(
            "저장 실패에 업로드 팝업 문구가 뜬다",
            0,
            composeRule.onAllNodesWithText(string(CoreUiR.string.core_ui_upload_error_title)).fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `다시 시도하기는 재시도 이벤트를 올린다`() {
        val events = mutableListOf<ReceiverHomeEvent>()
        setHomeContent(
            download = ReceiverDownloadState.FailedWithRetry(ReceiverDownloadErrorPopup.SERVER),
            onEvent = { events += it },
        )

        composeRule.onNodeWithText(string(CoreUiR.string.core_ui_server_error_retry)).performClick()

        assertEquals(listOf(ReceiverHomeEvent.RetryDownload), events)
    }

    /** 재시도해도 같은 실패(#1726)는 팝업이 아니라 종전 안내로 남는다. */
    @Test
    fun `재시도로 풀리지 않는 실패는 스낵바로 표출된다`() {
        setHomeContent(ReceiverDownloadState.Failed(R.string.home_receiver_download_all_failed))

        composeRule.onNodeWithText("모든 기록 내려받기에 실패했습니다.").assertIsDisplayed()
        assertEquals(
            "재시도가 답이 아닌 실패에 재시도 팝업이 떴다",
            0,
            composeRule.onAllNodesWithText(string(CoreUiR.string.core_ui_server_error_retry)).fetchSemanticsNodes().size,
        )
    }

    private fun string(resId: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(resId)

    private fun setHomeContent(
        download: ReceiverDownloadState,
        onEvent: (ReceiverHomeEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverHomeScreen(
                    uiState =
                        ReceiverHomeUiState.Success(
                            senderName = "박서연",
                            senderMessage = null,
                            mindRecord = null,
                            timeLetterTotalCount = null,
                            afternoteTotalCount = null,
                            afternoteIcons = emptyList(),
                            download = download,
                        ),
                    onEvent = onEvent,
                    actions = noopActions,
                )
            }
        }
    }
}
