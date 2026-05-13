package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.component.TimeLetterContent
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode

@Composable
fun TimeletterScreen(
    onWriteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TimeletterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ViewMode.List) }

    when (val state = uiState) {
        is TimeletterUiState.Loading -> {
            Unit
        }

        is TimeletterUiState.Error -> {
            Unit
        }

        is TimeletterUiState.Success -> {
            TimeLetterContent(
                letters = state.letters,
                receiverNameMap = state.receiverNameMap,
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onWriteClick = onWriteClick,
                modifier = modifier,
            )
        }
    }
}

private val previewLetters =
    TimeLetterList(
        timeLetters =
            listOf(
                TimeLetter(
                    id = 1L,
                    title = "미래의 나에게",
                    content = "지금 이 순간을 잊지 마. 열심히 살고 있는 너를 응원해.",
                    sendAt = "2026-12-31",
                    status = TimeLetterStatus.SCHEDULED,
                    mediaList = emptyList(),
                    receiverIds = listOf(1L),
                    createdAt = null,
                    updatedAt = null,
                ),
                TimeLetter(
                    id = 2L,
                    title = "10년 후의 나에게",
                    content = "지금보다 더 행복하길 바라.",
                    sendAt = "2035-01-01",
                    status = TimeLetterStatus.SCHEDULED,
                    mediaList = emptyList(),
                    receiverIds = listOf(2L),
                    createdAt = null,
                    updatedAt = null,
                ),
            ),
        totalCount = 2,
    )

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenEmptyPreview() {
    TimeLetterContent(
        letters = TimeLetterList(timeLetters = emptyList(), totalCount = 0),
        receiverNameMap = emptyMap(),
        viewMode = ViewMode.List,
        onViewModeChange = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenListPreview() {
    var viewMode by remember { mutableStateOf(ViewMode.List) }
    TimeLetterContent(
        letters = previewLetters,
        receiverNameMap = mapOf(1L to "박경민", 2L to "미래의 나"),
        viewMode = viewMode,
        onViewModeChange = { viewMode = it },
    )
}

@Preview(showBackground = true)
@Composable
private fun TimeletterScreenBlockPreview() {
    TimeLetterContent(
        letters = previewLetters,
        receiverNameMap = mapOf(1L to "박경민", 2L to "미래의 나"),
        viewMode = ViewMode.Block,
        onViewModeChange = {},
    )
}
