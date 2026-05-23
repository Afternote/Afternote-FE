package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.component.EmptyTimeLetterContent
import com.afternote.feature.timeletter.presentation.component.TimeLetterContent
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode

@Composable
fun TimeletterScreen(
    onWriteClick: () -> Unit = {},
    onLetterClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TimeletterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ViewMode.List) }

    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar() },
        floatingActionButton = { PenFloatingActionButton(onClick = onWriteClick) },
    ) { paddingValues ->
        when (val state = uiState) {
            is TimeletterUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is TimeletterUiState.Error -> {
                EmptyTimeLetterContent(modifier = Modifier.padding(paddingValues))
            }

            is TimeletterUiState.Success -> {
                TimeLetterContent(
                    letters = state.letters,
                    receiverNameMap = state.receiverNameMap,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    onLetterClick = onLetterClick,
                    modifier = Modifier.padding(paddingValues),
                )
            }
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
