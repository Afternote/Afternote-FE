package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.viewmodel.ReceiverEditUiState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun receiverEditScreenScreenshot() {
    ReceiverEditScreenScreenshotContent()
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun receiverEditScreenCompactScreenshot() {
    ReceiverEditScreenScreenshotContent()
}

@Composable
private fun ReceiverEditScreenScreenshotContent() {
    AfternoteTheme {
        ReceiverEditContent(
            uiState = ReceiverEditUiState(isLoading = false, receiver = receiverPreview),
            onBackClick = {},
            onRegister = { _, _, _, _, _ -> },
        )
    }
}

private val receiverPreview =
    ReceiverDetail(
        receiverId = 1L,
        name = "박경민",
        relation = "친구",
        phone = "010-1234-5678",
        email = "friend@afternote.kr",
        dailyQuestionCount = 3,
        timeLetterCount = 2,
        afterNoteCount = 1,
        message = "오랫동안 기억해 줘",
        authCode = "AFTER123",
    )
