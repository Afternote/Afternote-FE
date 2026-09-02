package com.afternote.feature.home.presentation.receiver

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.R
import com.afternote.feature.home.presentation.receiver.model.ReceiverDownloadState
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 이 테스트의 관심 밖인 외부 라우팅을 채우는 no-op 묶음. */
private val noopActions =
    ReceiverHomeActions(
        onNavigateToMindRecord = {},
        onNavigateToTimeLetter = {},
        onNavigateToAfternote = {},
    )

/**
 * 수신자 홈 내려받기 실패 안내의 Snackbar 표출 가드 (#1391).
 *
 * 종전 Toast 는 Compose semantics 에 잡히지 않아 어떤 테스트로도 단언할 수 없었다 — 이 단언이
 * 가능해진 것 자체가 전환 목적의 일부다. 문구는 리소스 원문으로 대조한다 (모듈 테스트 관례,
 * [com.afternote.feature.receiver.presentation.deliveryverification.ReceiverVerificationTest] 참조).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReceiverHomeDownloadSnackbarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `내려받기 실패 상태는 스낵바로 표출된다`() {
        setHomeContent(ReceiverDownloadState.Failed(R.string.home_receiver_download_all_failed))

        composeRule.onNodeWithText("모든 기록 내려받기에 실패했습니다.").assertIsDisplayed()
    }

    @Test
    fun `파일 저장 실패 상태도 자기 문구로 표출된다`() {
        setHomeContent(ReceiverDownloadState.Failed(R.string.home_receiver_download_all_save_failed))

        composeRule.onNodeWithText("파일 저장에 실패했습니다.").assertIsDisplayed()
    }

    private fun setHomeContent(download: ReceiverDownloadState) {
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
                    onEvent = { },
                    actions = noopActions,
                )
            }
        }
    }
}
