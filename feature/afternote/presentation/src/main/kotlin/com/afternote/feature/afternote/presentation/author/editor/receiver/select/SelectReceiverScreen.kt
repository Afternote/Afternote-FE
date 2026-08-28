package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.receiver.ReceiverSelectItem
import com.afternote.core.ui.receiver.ReceiverSelectScreen
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

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
        title = stringResource(R.string.afternote_select_receiver_title),
        searchPlaceholder = stringResource(R.string.afternote_select_receiver_search_placeholder),
        confirmText = stringResource(R.string.afternote_select_receiver_confirm),
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
                    { SelectReceiverLoading() }
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
private fun SelectReceiverLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
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

@Preview(showBackground = true)
@Composable
private fun SelectReceiverScreenPreview() {
    AfternoteTheme {
        SelectReceiverScreen(
            uiState =
                SelectReceiverUiState(
                    receivers =
                        listOf(
                            AfternoteEditorReceiver(id = 1L, name = "김혜성", label = "아들"),
                            AfternoteEditorReceiver(id = 2L, name = "박경민", label = "친구"),
                            AfternoteEditorReceiver(id = 3L, name = "이영희", label = "연인"),
                        ),
                    selectedReceiverId = 1L,
                ),
            onBackClick = {},
            onReceiverToggle = {},
            onRetryClick = {},
            onConfirmClick = {},
        )
    }
}
