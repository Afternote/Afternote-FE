package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.model.InquiryStatus
import com.afternote.feature.setting.presentation.model.InquiryUiModel
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun inquiryWriteScreenshot() {
    AfternoteTheme { InquiryWriteScreen(onBackClick = {}) }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun inquiryAnsweredListScreenshot() {
    AfternoteTheme {
        InquiryListScreen(
            inquiries = listOf(InquiryUiModel(1L, InquiryStatus.ANSWERED, "2026.09.08", "문의 제목", "문의 내용", "답변 내용")),
            onBackClick = {},
            onInquiryClick = {},
            onNewInquiryClick = {},
        )
    }
}
