package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.R
import com.afternote.feature.timeletter.presentation.component.TimeLetterContent
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterViewModel
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode

@Composable
fun TimeletterScreen(
    onLetterClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    onSettingClick: () -> Unit = {},
    onWriteClick: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    onFilterRecipientClick: () -> Unit = {},
    viewModel: TimeletterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(ViewMode.List) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailureMessage = stringResource(R.string.timeletter_delete_failure)

    pendingDeleteId?.let { timeLetterId ->
        Popup(
            type = PopupType.Variant2,
            message = stringResource(R.string.timeletter_delete_confirm_message),
            confirmText = stringResource(R.string.timeletter_delete_confirm),
            dismissText = stringResource(R.string.timeletter_delete_dismiss),
            onConfirm = {
                pendingDeleteId = null
                viewModel.deleteTimeLetter(timeLetterId)
            },
            onDismiss = { pendingDeleteId = null },
        )
    }

    val showDeleteFailure = (uiState as? TimeletterUiState.Success)?.showDeleteFailure == true
    LaunchedEffect(showDeleteFailure) {
        if (showDeleteFailure) {
            snackbarHostState.showSnackbar(
                message = deleteFailureMessage,
                withDismissAction = true,
            )
            viewModel.consumeDeleteFailure()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.load()
        }
    }

    TimeletterScreenContent(
        uiState = uiState,
        viewMode = viewMode,
        onViewModeChange = { viewMode = it },
        snackbarHostState = snackbarHostState,
        onLetterClick = onLetterClick,
        onSettingClick = onSettingClick,
        onWriteClick = onWriteClick,
        onEditClick = onEditClick,
        onFilterRecipientClick = onFilterRecipientClick,
        onDeleteClick = { pendingDeleteId = it },
        modifier = modifier,
    )
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
