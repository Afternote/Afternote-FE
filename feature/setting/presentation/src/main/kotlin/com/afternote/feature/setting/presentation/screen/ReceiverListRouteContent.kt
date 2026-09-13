package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListLoadState
import com.afternote.feature.setting.presentation.viewmodel.ReceiverListUiState

/** 조회 상태는 선택·관리 두 진입에서 동일하게 소비하고, 성공 시 기존 화면 동작을 유지한다. */
@Composable
internal fun ReceiverListRouteContent(
    uiState: ReceiverListUiState,
    selectForDeliveryConditions: Boolean,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onConfirmClick: (ReceiverListItem) -> Unit,
    onReceiverClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInitialLoading = uiState.loadState == ReceiverListLoadState.Loading && uiState.receivers.isEmpty()
    val listReplacement: (@Composable () -> Unit)? =
        if (isInitialLoading || uiState.loadState == ReceiverListLoadState.InitialFailure) {
            {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isInitialLoading) {
                        LoadingBody()
                    } else {
                        ReceiverListRetry(onRetry = onRetry)
                    }
                }
            }
        } else {
            null
        }

    Column(modifier = modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (selectForDeliveryConditions) {
                ReceiverListScreen(
                    receivers = uiState.receivers,
                    onBackClick = onBackClick,
                    onConfirmClick = onConfirmClick,
                    listReplacement = listReplacement,
                )
            } else {
                ReceiverManageScreen(
                    receivers = uiState.receivers,
                    onBackClick = onBackClick,
                    onReceiverClick = onReceiverClick,
                    listReplacement = listReplacement,
                )
            }
        }
        when (uiState.loadState) {
            ReceiverListLoadState.Loading -> {
                if (!isInitialLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            ReceiverListLoadState.RefreshFailure -> {
                ReceiverListRetry(onRetry = onRetry)
            }

            ReceiverListLoadState.Ready, ReceiverListLoadState.InitialFailure -> {
                Unit
            }
        }
    }
}

@Composable
private fun ReceiverListRetry(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.setting_receiver_list_load_failed))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.setting_receiver_list_retry))
        }
    }
}
