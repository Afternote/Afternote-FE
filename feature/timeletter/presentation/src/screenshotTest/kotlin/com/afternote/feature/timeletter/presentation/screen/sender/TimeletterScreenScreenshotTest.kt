package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun timeletterScreenEmptyScreenshot() {
    TimeletterScreenScreenshotContent(
        letters = TimeLetterList(timeLetters = emptyList(), totalCount = 0),
        viewMode = ViewMode.List,
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun timeletterScreenListScreenshot() {
    TimeletterScreenScreenshotContent(
        letters = previewLetters,
        viewMode = ViewMode.List,
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun timeletterScreenBlockScreenshot() {
    TimeletterScreenScreenshotContent(
        letters = previewLetters,
        viewMode = ViewMode.Block,
    )
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun timeletterScreenBlockCompactScreenshot() {
    TimeletterScreenScreenshotContent(
        letters = previewLetters,
        viewMode = ViewMode.Block,
    )
}

@Composable
private fun TimeletterScreenScreenshotContent(
    letters: TimeLetterList,
    viewMode: ViewMode,
) {
    AfternoteTheme {
        TimeletterScreenContent(
            uiState =
                TimeletterUiState.Success(
                    letters = letters,
                    receiverNameMap = mapOf(1L to "박경민", 2L to "미래의 나"),
                ),
            viewMode = viewMode,
            onViewModeChange = {},
            snackbarHostState = remember { SnackbarHostState() },
            onLetterClick = {},
            onSettingClick = {},
            onWriteClick = {},
            onEditClick = {},
            onFilterRecipientClick = {},
            onDeleteClick = {},
        )
    }
}

private val previewLetters =
    TimeLetterList(
        timeLetters =
            listOf(
                TimeLetter(
                    id = 1L,
                    title = "미래의 나에게",
                    sendAt = "2026-12-31T00:00:00",
                    deliveredAt = null,
                    status = TimeLetterStatus.SCHEDULED,
                    blocks = emptyList(),
                    receiverIds = listOf(1L),
                ),
                TimeLetter(
                    id = 2L,
                    title = "10년 후의 나에게",
                    sendAt = "2035-01-01T00:00:00",
                    deliveredAt = null,
                    status = TimeLetterStatus.SCHEDULED,
                    blocks = emptyList(),
                    receiverIds = listOf(2L),
                ),
            ),
        totalCount = 2,
    )
