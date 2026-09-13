package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteUiState
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDate

/**
 * 키보드가 없어도 스크롤되지 않는 일기 작성 화면의 기본 레이아웃을 고정한다 (#1131).
 *
 * 실제 상태와 같은 제목·기분·본문·임시저장 수를 넣어 활성 등록 버튼과 에디터 툴바까지
 * baseline 에 포함한다. 좁은 기기에서는 [COMPACT_DEVICE_SPEC] 변형이 세로 잘림을 감시한다.
 */
@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
internal fun diaryWriteScreenScreenshot() {
    AfternoteTheme {
        DiaryWriteScreenContent(
            uiState = diaryWriteScreenshotState(),
            modifier = Modifier.fillMaxSize(),
            onBackClick = {},
            onContentChanged = {},
            onDraftListClick = {},
            onMoodSelected = {},
            onReceiverRowClick = {},
            onDateRowClick = {},
            onSaveDraft = {},
            onSubmit = {},
            onTitleChanged = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun diaryWriteScreenCompactScreenshot() {
    AfternoteTheme {
        DiaryWriteScreenContent(
            uiState = diaryWriteScreenshotState(),
            modifier = Modifier.fillMaxSize(),
            onBackClick = {},
            onContentChanged = {},
            onDraftListClick = {},
            onMoodSelected = {},
            onReceiverRowClick = {},
            onDateRowClick = {},
            onSaveDraft = {},
            onSubmit = {},
            onTitleChanged = {},
        )
    }
}

private fun diaryWriteScreenshotState(): DiaryWriteUiState =
    DiaryWriteUiState(
        title = "비 오는 날의 기록",
        content = "<p>창문을 두드리는 빗소리를 오래 들었다.</p>",
        mood = TodayMood.HAPPY,
        date = LocalDate.of(2026, 8, 28),
        draftCount = 2,
    )
