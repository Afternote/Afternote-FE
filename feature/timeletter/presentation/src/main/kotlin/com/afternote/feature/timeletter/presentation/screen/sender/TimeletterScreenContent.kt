package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.afternote.core.ui.button.FAB.PenFloatingActionButton
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.timeletter.presentation.component.EmptyTimeLetterContent
import com.afternote.feature.timeletter.presentation.component.TimeLetterContent
import com.afternote.feature.timeletter.presentation.viewmodel.TimeletterUiState
import com.afternote.feature.timeletter.presentation.viewmodel.ViewMode

/**
 * 타임레터 목록 화면의 상태 없는 본문.
 *
 * ViewModel·부수효과는 [TimeletterScreen] 이 들고, 이 함수는 넘겨받은 상태만 그린다.
 * screenshotTest 가 각 상태를 고정 입력으로 렌더하기 위한 경계다.
 */
@Composable
internal fun TimeletterScreenContent(
    uiState: TimeletterUiState,
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    snackbarHostState: SnackbarHostState,
    onLetterClick: (Long) -> Unit,
    onSettingClick: () -> Unit,
    onWriteClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onFilterRecipientClick: () -> Unit,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = { HomeTopBar(onSettingClick = onSettingClick) },
        floatingActionButton = { PenFloatingActionButton(onClick = onWriteClick) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        when (uiState) {
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
                    letters = uiState.letters,
                    receiverNameMap = uiState.receiverNameMap,
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange,
                    selectedFilterReceiverIds = uiState.selectedFilterReceiverIds,
                    onFilterClick = onFilterRecipientClick,
                    onLetterClick = onLetterClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}
