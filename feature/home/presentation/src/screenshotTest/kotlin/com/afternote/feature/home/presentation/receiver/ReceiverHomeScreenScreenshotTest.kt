package com.afternote.feature.home.presentation.receiver

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.icon.AfternoteSourceIcon
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.receiver.model.MindRecordSummary
import com.afternote.feature.home.presentation.receiver.model.ReceiverHomeUiState
import com.afternote.feature.home.presentation.receiver.model.SenderMessage
import com.android.tools.screenshot.PreviewTest

/** baseline 은 화면 픽셀만 본다 — 외부 라우팅은 눌리지 않으므로 빈 액션으로 채운다. */
private val noopActions =
    ReceiverHomeActions(
        onNavigateToMindRecord = {},
        onNavigateToTimeLetter = {},
        onNavigateToAfternote = {},
    )

/**
 * [ReceiverHomeScreen] 의 시각 회귀 baseline — Loading + Success + 부분 실패 세 케이스.
 *
 * 의도된 시각 변경 시 `./gradlew :app:updateDebugScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverHomeScreenLoadingScreenshot() {
    AfternoteTheme {
        ReceiverHomeScreen(
            uiState = ReceiverHomeUiState.Loading,
            onEvent = {},
            actions = noopActions,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverHomeScreenSuccessScreenshot() {
    AfternoteTheme {
        ReceiverHomeScreen(
            uiState =
                ReceiverHomeUiState.Success(
                    senderName = "서연",
                    senderMessage =
                        SenderMessage(
                            date = "2026.04.04",
                            body = "내가 없어도 너의 시간이 멈추지 않고\n행복하게 흘러갔으면 좋겠어.",
                        ),
                    mindRecord =
                        MindRecordSummary(
                            dailyQuestionCount = 10,
                            diaryCount = 8,
                        ),
                    timeLetterTotalCount = 3,
                    afternoteTotalCount = 5,
                    afternoteIcons =
                        listOf(
                            AfternoteSourceIcon.SocialNetwork,
                        ),
                ),
            onEvent = {},
            actions = noopActions,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverHomeScreenPartialFailureScreenshot() {
    AfternoteTheme {
        ReceiverHomeScreen(
            uiState =
                ReceiverHomeUiState.Success(
                    senderName = "서연",
                    senderMessage = null,
                    mindRecord = null,
                    timeLetterTotalCount = 3,
                    afternoteTotalCount = null,
                    afternoteIcons = emptyList(),
                ),
            onEvent = {},
            actions = noopActions,
        )
    }
}
