package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteUiState
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDate

/**
 * 추천 질문 배너와 에디터가 함께 있는 데일리질문 작성 화면을 고정한다 (#1131).
 *
 * 현재 날짜는 Preview 밖에서 주입해 실행일에 따라 baseline 이 흔들리지 않게 한다. 좁은 기기에서는
 * [COMPACT_DEVICE_SPEC] 변형이 배너 줄바꿈과 하단 툴바의 세로 잘림을 함께 감시한다.
 */
@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
internal fun dailyQuestionWriteScreenScreenshot() {
    AfternoteTheme {
        DailyQuestionWriteScreenContent(
            uiState = dailyQuestionWriteScreenshotState(),
            date = SCREENSHOT_DATE,
            modifier = Modifier.fillMaxSize(),
            onAnswerChanged = {},
            onBackClick = {},
            onDraftListClick = {},
            onRetryResumeDraft = {},
            onSaveDraft = {},
            onSubmit = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun dailyQuestionWriteScreenCompactScreenshot() {
    AfternoteTheme {
        DailyQuestionWriteScreenContent(
            uiState = dailyQuestionWriteScreenshotState(),
            date = SCREENSHOT_DATE,
            modifier = Modifier.fillMaxSize(),
            onAnswerChanged = {},
            onBackClick = {},
            onDraftListClick = {},
            onRetryResumeDraft = {},
            onSaveDraft = {},
            onSubmit = {},
        )
    }
}

private fun dailyQuestionWriteScreenshotState(): DailyQuestionWriteUiState =
    DailyQuestionWriteUiState(
        questionId = 28L,
        questionDay = 28,
        questionContent = "오늘 가장 오래 기억하고 싶은 순간은 무엇인가요?",
        answer = "<p>가족과 늦은 저녁을 먹으며 웃었던 순간.</p>",
        isQuestionLoading = false,
        draftCount = 1,
    )

private val SCREENSHOT_DATE = LocalDate.of(2026, 8, 28)
