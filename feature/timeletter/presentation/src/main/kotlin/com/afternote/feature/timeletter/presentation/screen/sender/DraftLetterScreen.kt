package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.R
import com.afternote.feature.timeletter.presentation.component.DraftLetterItem
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.DraftLetterViewModel

@Composable
fun DraftLetterScreen(
    onBackClick: () -> Unit,
    onOpenDraft: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DraftLetterViewModel = hiltViewModel(),
    refreshRequested: Boolean = false,
    onRefreshConsumed: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val messageRes = (uiState as? DraftLetterUiState.Success)?.messageRes
    val message = messageRes?.let { stringResource(it) }

    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            onRefreshConsumed()
            viewModel.loadDrafts()
        }
    }

    LaunchedEffect(messageRes) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onMessageShown()
        }
    }

    DraftLetterContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onOpenDraft = onOpenDraft,
        onEditCompleteClick = viewModel::toggleEditMode,
        onToggleSelection = viewModel::toggleSelection,
        onDeleteAll = viewModel::deleteAll,
        onDeleteSelected = viewModel::deleteSelected,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun DraftLetterContent(
    uiState: DraftLetterUiState,
    onBackClick: () -> Unit,
    onOpenDraft: (Long) -> Unit,
    onEditCompleteClick: () -> Unit,
    onToggleSelection: (Long) -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
) {
    val successState = uiState as? DraftLetterUiState.Success
    val isEditMode = successState?.isEditMode == true

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.timeletter_draft_title),
                onBackClick = onBackClick,
                actions = {
                    if (successState != null && successState.drafts.isNotEmpty()) {
                        TimeLetterTextButton(
                            text = stringResource(if (isEditMode) R.string.timeletter_draft_done else R.string.timeletter_draft_edit),
                            isActive = isEditMode,
                            onClick = onEditCompleteClick,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (isEditMode) {
                AfternoteButton(
                    text = stringResource(R.string.timeletter_draft_delete_all),
                    onClick = onDeleteAll,
                    secondaryText = stringResource(R.string.timeletter_draft_delete_selected),
                    onSecondaryClick = onDeleteSelected,
                    type = AfternoteButtonType.Variant5,
                    isLoading = successState.isDeleting,
                    isSecondaryEnabled = successState.isDeleteSelectedEnabled,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        when (uiState) {
            DraftLetterUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }

            is DraftLetterUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(uiState.messageRes),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray6,
                    )
                }
            }

            is DraftLetterUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    Text(
                        text =
                            stringResource(
                                if (uiState.isEditMode) R.string.timeletter_draft_total_selected else R.string.timeletter_draft_total,
                                if (uiState.isEditMode) uiState.selectedIds.size else uiState.drafts.size,
                            ),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray9,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    if (uiState.drafts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.timeletter_draft_empty),
                                style = AfternoteDesign.typography.captionLargeR,
                                color = AfternoteDesign.colors.gray6,
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(items = uiState.drafts, key = { it.id }) { draft ->
                                DraftLetterItem(
                                    draft = draft,
                                    receiverNameMap = uiState.receiverNameMap,
                                    isEditMode = uiState.isEditMode,
                                    isSelected = draft.id in uiState.selectedIds,
                                    onOpen = { onOpenDraft(draft.id) },
                                    onToggle = { onToggleSelection(draft.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DraftLetterScreenPreview() {
    DraftLetterContent(
        uiState =
            DraftLetterUiState.Success(
                drafts =
                    listOf(
                        TimeLetter(1L, "첫 번째 레터", "2026-12-25T00:00:00", null, TimeLetterStatus.DRAFT, emptyList(), listOf(1L)),
                        TimeLetter(2L, "두 번째 레터", null, null, TimeLetterStatus.DRAFT, emptyList(), listOf(2L)),
                    ),
                receiverNameMap = mapOf(1L to "김지은", 2L to "이현우"),
            ),
        onBackClick = {},
        onOpenDraft = {},
        onEditCompleteClick = {},
        onToggleSelection = {},
        onDeleteAll = {},
        onDeleteSelected = {},
    )
}
