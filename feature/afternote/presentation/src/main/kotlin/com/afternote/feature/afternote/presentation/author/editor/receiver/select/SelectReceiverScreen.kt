package com.afternote.feature.afternote.presentation.author.editor.receiver.select

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.common.util.KoreanConsonantUtil
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.KoreanConsonantIndex
import com.afternote.core.ui.ProfileImage
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import kotlinx.coroutines.launch

/**
 * 애프터노트 에디터의 수신자 선택 화면 (#540, 시안 3631:24820).
 *
 * 검색 필드 + 초성 인덱스(ㄱ~ㅎ) + 단일 선택 + 하단 "수신자 선택 완료하기".
 * 구조는 설정의 수신자 목록 화면(`ReceiverListScreen`)과 같은 core:ui 부품으로 조립한다 —
 * 공용 컴포넌트 추출은 #791 몫이라 여기서는 조립만 하고 새 공용 부품을 만들지 않는다.
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
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.afternote_select_receiver_title),
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            AfternoteButton(
                text = stringResource(R.string.afternote_select_receiver_confirm),
                onClick = { uiState.selectedReceiverId?.let(onConfirmClick) },
                type = if (uiState.selectedReceiverId != null) AfternoteButtonType.Default else AfternoteButtonType.Un,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = 10.dp),
        ) {
            SelectReceiverContent(
                uiState = uiState,
                onReceiverToggle = onReceiverToggle,
                onRetryClick = onRetryClick,
            )
        }
    }
}

@Composable
private fun SelectReceiverContent(
    uiState: SelectReceiverUiState,
    onReceiverToggle: (Long) -> Unit,
    onRetryClick: () -> Unit,
) {
    val searchState = rememberTextFieldState()

    AfternoteTextField(
        state = searchState,
        placeholder = stringResource(R.string.afternote_select_receiver_search_placeholder),
        type = TextFieldType.Search,
        imeAction = ImeAction.Search,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
    when {
        uiState.loadFailed -> {
            SelectReceiverLoadFailed(onRetryClick = onRetryClick)
        }

        uiState.isLoading && uiState.receivers.isEmpty() -> {
            SelectReceiverLoading()
        }

        uiState.receivers.isEmpty() -> {
            SelectReceiverEmpty()
        }

        else -> {
            SelectReceiverList(
                receivers = uiState.receivers,
                searchQuery = searchState.text.toString(),
                selectedReceiverId = uiState.selectedReceiverId,
                onReceiverToggle = onReceiverToggle,
            )
        }
    }
}

@Composable
private fun SelectReceiverList(
    receivers: List<AfternoteEditorReceiver>,
    searchQuery: String,
    selectedReceiverId: Long?,
    onReceiverToggle: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedConsonant by remember { mutableStateOf<Char?>(null) }

    val groupedReceivers =
        remember(receivers, searchQuery) {
            val filtered =
                if (searchQuery.isBlank()) receivers else receivers.filter { it.name.contains(searchQuery) }
            KoreanConsonantUtil.groupByInitialConsonant(filtered) { it.name }
        }

    val consonantIndexMap =
        remember(groupedReceivers) {
            var index = 0
            buildMap {
                groupedReceivers.forEach { (consonant, items) ->
                    put(consonant, index)
                    index += items.size
                }
            }
        }

    LaunchedEffect(listState, consonantIndexMap) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                selectedConsonant =
                    consonantIndexMap.entries
                        .filter { it.value <= firstVisibleItemIndex }
                        .maxByOrNull { it.value }
                        ?.key
            }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
        ) {
            groupedReceivers.forEach { (_, items) ->
                items(items, key = { it.id }) { receiver ->
                    SelectReceiverListItem(
                        receiver = receiver,
                        selected = receiver.id == selectedReceiverId,
                        onToggle = { onReceiverToggle(receiver.id) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.padding(14.dp)) }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            KoreanConsonantIndex(
                selectedConsonant = selectedConsonant,
                onConsonantSelect = { consonant ->
                    selectedConsonant = consonant
                    consonantIndexMap[consonant]?.let { index ->
                        coroutineScope.launch { listState.scrollToItem(index) }
                    }
                },
            )
        }
    }
}

@Composable
private fun SelectReceiverListItem(
    receiver: AfternoteEditorReceiver,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileImage(size = 50.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receiver.name,
                style = AfternoteDesign.typography.captionLargeB,
            )
            Spacer(modifier = Modifier.padding(top = 5.dp))
            Text(
                text = receiver.label,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray8,
            )
        }
        AfternoteCircularCheckbox(
            state = if (selected) CheckboxState.Default else CheckboxState.None,
            onClick = onToggle,
            size = 20.dp,
        )
    }
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
