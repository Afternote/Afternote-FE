package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.feature.timeletter.domain.LetterIdentity
import com.afternote.feature.timeletter.domain.LetterSchedule
import com.afternote.feature.timeletter.domain.OpenDate
import com.afternote.feature.timeletter.domain.TimeLetter
import com.afternote.feature.timeletter.domain.TimeLetters
import com.afternote.feature.timeletter.presentation.component.TimeLetterContent
import com.afternote.feature.timeletter.presentation.component.ViewMode

@Composable
fun TimeletterScreen(modifier: Modifier = Modifier) {
    var viewMode by remember { mutableStateOf(ViewMode.List) }

    TimeLetterContent(
        letters = TimeLetters(emptyList()),
        viewMode = viewMode,
        onViewModeChange = { viewMode = it },
        modifier = modifier,
    )
}

private val previewLetters =
    TimeLetters(
        listOf(
            TimeLetter(
                identity =
                    LetterIdentity(
                        id = 1L,
                        title = "미래의 나에게",
                        body = "지금 이 순간을 잊지 마. 열심히 살고 있는 너를 응원해.",
                    ),
                schedule = LetterSchedule(recipientName = "박경민", openDate = OpenDate("2026-12-31")),
            ),
            TimeLetter(
                identity = LetterIdentity(id = 2L, title = "10년 후의 나에게", body = "지금보다 더 행복하길 바라."),
                schedule =
                    LetterSchedule(
                        recipientName = "미래의 나",
                        openDate = OpenDate("2035-01-01"),
                    ),
            ),
        ),
    )

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenEmptyPreview() {
    TimeletterScreen()
}

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenListPreview() {
    var viewMode by remember { mutableStateOf(ViewMode.List) }
    TimeLetterContent(
        letters = previewLetters,
        viewMode = viewMode,
        onViewModeChange = { viewMode = it },
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenBlockPreview() {
    TimeLetterContent(
        letters = previewLetters,
        viewMode = ViewMode.Block,
        onViewModeChange = {},
    )
}
