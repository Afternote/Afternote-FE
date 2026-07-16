package com.afternote.afternote_fe.screen.receiver

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.afternote_fe.screen.receiver.model.AfternoteSourceIcon
import com.afternote.afternote_fe.screen.receiver.model.MindRecordSummary
import com.afternote.afternote_fe.screen.receiver.model.ReceiverHomeUiState
import com.afternote.afternote_fe.screen.receiver.model.SenderMessage
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [ReceiverHomeScreen] 의 시각 회귀 baseline — Loading + Success 두 케이스.
 *
 * 의도된 시각 변경 시 `./gradlew :app:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverHomeScreenLoadingScreenshot() {
    AfternoteTheme {
        ReceiverHomeScreen(
            uiState = ReceiverHomeUiState.Loading,
            onEvent = {},
            actions = ReceiverHomeActions.Noop,
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
                            totalCount = 24,
                            dailyQuestionCount = 10,
                            diaryCount = 8,
                        ),
                    timeLetterTotalCount = 3,
                    afternoteTotalCount = 5,
                    afternoteIcons =
                        listOf(
                            AfternoteSourceIcon(drawableResId = com.afternote.core.ui.R.drawable.core_ui_ic_tabler_search),
                        ),
                ),
            onEvent = {},
            actions = ReceiverHomeActions.Noop,
        )
    }
}
