package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.component.DraftLetterItem
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterViewModel

@Composable
fun DraftLetterScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DraftLetterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DraftLetterContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onEditCompleteClick = {
            val current = uiState as? DraftLetterUiState.Success ?: return@DraftLetterContent
            if (current.isEditMode && current.selectedIds.isNotEmpty()) {
                viewModel.deleteSelected()
            } else {
                viewModel.toggleEditMode()
            }
        },
        onToggleSelection = viewModel::toggleSelection,
        modifier = modifier,
    )
}

@Composable
private fun DraftLetterContent(
    uiState: DraftLetterUiState,
    onBackClick: () -> Unit,
    onEditCompleteClick: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEditMode = (uiState as? DraftLetterUiState.Success)?.isEditMode ?: false
    val selectedIds = (uiState as? DraftLetterUiState.Success)?.selectedIds ?: emptySet()

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "임시저장된 레터",
                onBackClick = onBackClick,
                actions = {
                    TimeLetterTextButton(
                        text = when {
                            !isEditMode -> "수정"
                            selectedIds.isEmpty() -> "취소"
                            else -> "삭제"
                        },
                        isActive = isEditMode && selectedIds.isNotEmpty(),
                        onClick = onEditCompleteClick,
                    )
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is DraftLetterUiState.Loading -> {
                Unit
            }

            is DraftLetterUiState.Error -> {
                Unit
            }

            is DraftLetterUiState.Success -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    items(
                        items = uiState.drafts,
                        key = { it.id },
                    ) { draft ->
                        DraftLetterItem(
                            draft = draft,
                            isEditMode = isEditMode,
                            isSelected = draft.id in selectedIds,
                            onToggle = { onToggleSelection(draft.id) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "일반 - 목록 있음")
@Composable
private fun DraftLetterScreenNormalPreview() {
    DraftLetterContent(
        uiState =
            DraftLetterUiState.Success(
                drafts =
                    listOf(
                        TimeLetter(
                            id = 1L,
                            title = "첫 번째 레터",
                            sendAt = "2026-12-25T00:00:00",
                            deliveredAt = null,
                            status = TimeLetterStatus.DRAFT,
                            blocks = emptyList(),
                            receiverIds = listOf(1L),
                        ),
                        TimeLetter(
                            id = 2L,
                            title = "두 번째 레터",
                            sendAt = "2026-06-01T00:00:00",
                            deliveredAt = null,
                            status = TimeLetterStatus.DRAFT,
                            blocks = emptyList(),
                            receiverIds = listOf(2L),
                        ),
                        TimeLetter(
                            id = 3L,
                            title = null,
                            sendAt = null,
                            deliveredAt = null,
                            status = TimeLetterStatus.DRAFT,
                            blocks = emptyList(),
                            receiverIds = emptyList(),
                        ),
                    ),
            ),
        onBackClick = {},
        onEditCompleteClick = {},
        onToggleSelection = {},
    )
}

@Preview(showBackground = true, name = "수정 모드 - 항목 선택됨")
@Composable
private fun DraftLetterScreenEditModePreview() {
    DraftLetterContent(
        uiState =
            DraftLetterUiState.Success(
                drafts =
                    listOf(
                        TimeLetter(
                            id = 1L,
                            title = "첫 번째 레터",
                            sendAt = "2026-12-25T00:00:00",
                            deliveredAt = null,
                            status = TimeLetterStatus.DRAFT,
                            blocks = emptyList(),
                            receiverIds = listOf(1L),
                        ),
                        TimeLetter(
                            id = 2L,
                            title = "두 번째 레터",
                            sendAt = "2026-06-01T00:00:00",
                            deliveredAt = null,
                            status = TimeLetterStatus.DRAFT,
                            blocks = emptyList(),
                            receiverIds = listOf(2L),
                        ),
                    ),
                isEditMode = true,
                selectedIds = setOf(1L),
            ),
        onBackClick = {},
        onEditCompleteClick = {},
        onToggleSelection = {},
    )
}
