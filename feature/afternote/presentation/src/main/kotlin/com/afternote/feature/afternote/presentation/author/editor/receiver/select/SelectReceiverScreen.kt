package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.receiver.ReceiverSelectItem
import com.afternote.core.ui.receiver.ReceiverSelectScreen
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R

/**
 * 애프터노트 에디터의 수신자 선택 화면 (#540, 시안 3631:24820).
 *
 * 검색·초성 인덱스·단일 선택·완료 UI 는 공용 [ReceiverSelectScreen](#791) 이 그리고,
 * 여기서는 에디터 모델 매핑과 로딩·조회 실패·빈 목록 상태만 소유한다.
 */
@Composable
internal fun SelectReceiverScreen(
    uiState: SelectReceiverUiState,
    onBackClick: () -> Unit,
    onReceiverToggle: (Long) -> Unit,
    onRetryClick: () -> Unit,
    onConfirmClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReceiverSelectScreen(
        receivers =
            remember(uiState.receivers) {
                uiState.receivers.map { ReceiverSelectItem(id = it.id, name = it.name, relation = it.label) }
            },
        selectedReceiverId = uiState.selectedReceiverId,
        onReceiverToggle = onReceiverToggle,
        onBackClick = onBackClick,
        onConfirmClick = onConfirmClick,
        modifier = modifier,
        listReplacement =
            when {
                uiState.loadFailed -> {
                    { SelectReceiverLoadFailed(onRetryClick = onRetryClick) }
                }

                uiState.isLoading && uiState.receivers.isEmpty() -> {
                    { LoadingBody() }
                }

                uiState.receivers.isEmpty() -> {
                    { SelectReceiverEmpty() }
                }

                else -> {
                    null
                }
            },
    )
}

@Composable
private fun SelectReceiverEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.afternote_select_receiver_empty),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SelectReceiverLoadFailed(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.afternote_select_receiver_load_failed),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray8,
            textAlign = TextAlign.Center,
        )
        AfternoteButton(
            text = stringResource(R.string.afternote_select_receiver_retry),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp),
        )
    }
}
