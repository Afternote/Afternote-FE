package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.common.util.KoreanConsonantUtil
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.KoreanConsonantIndex
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.presentation.R
import com.afternote.feature.timeletter.presentation.component.RecipientListItem
import com.afternote.feature.timeletter.presentation.component.TimeLetterLoadErrorContent
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientListUiState
import com.afternote.feature.timeletter.presentation.viewmodel.RecipientListViewModel
import kotlinx.coroutines.launch

@Composable
fun RecipientListScreen(
    onBackClick: () -> Unit,
    onConfirmClick: (List<ReceiverListItem>) -> Unit,
    modifier: Modifier = Modifier,
    allowEmptyConfirm: Boolean = false,
    viewModel: RecipientListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        RecipientListUiState.Loading -> {
            RecipientListLoadingContent(onBackClick, modifier)
        }

        RecipientListUiState.Error -> {
            Scaffold(
                modifier = modifier,
                topBar = { DetailTopBar(title = "수신자 목록", onBackClick = onBackClick) },
            ) { innerPadding ->
                TimeLetterLoadErrorContent(
                    message = stringResource(R.string.timeletter_recipient_list_load_failed),
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        is RecipientListUiState.Success -> {
            RecipientListContent(
                recipients = state.recipients,
                onBackClick = onBackClick,
                onConfirmClick = onConfirmClick,
                allowEmptyConfirm = allowEmptyConfirm,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun RecipientListLoadingContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = "수신자 목록", onBackClick = onBackClick) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun RecipientListContent(
    recipients: List<ReceiverListItem>,
    onBackClick: () -> Unit,
    onConfirmClick: (List<ReceiverListItem>) -> Unit,
    modifier: Modifier = Modifier,
    allowEmptyConfirm: Boolean = false,
) {
    val searchState = rememberTextFieldState()
    val selectedIds = remember { mutableStateSetOf<Long>() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedConsonant by remember { mutableStateOf<Char?>(null) }

    val groupedRecipients =
        remember(recipients, searchState.text) {
            val query = searchState.text.toString()
            val filtered =
                if (query.isBlank()) recipients else recipients.filter { it.name.contains(query) }
            KoreanConsonantUtil.groupByInitialConsonant(filtered) { it.name }
        }

    val consonantIndexMap =
        remember(groupedRecipients) {
            var index = 0
            buildMap {
                groupedRecipients.forEach { (consonant, items) ->
                    put(consonant, index)
                    index += 1 + items.size
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

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "수신인 목록",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            AfternoteButton(
                text = "수신자 선택 완료하기",
                onClick = {
                    onConfirmClick(recipients.filter { it.receiverId in selectedIds })
                },
                type = if (selectedIds.isNotEmpty() || allowEmptyConfirm) AfternoteButtonType.Default else AfternoteButtonType.Un,
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
            AfternoteTextField(
                state = searchState,
                placeholder = stringResource(R.string.timeletter_recipient_search_placeholder),
                type = TextFieldType.Search,
                imeAction = ImeAction.Search,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 20.dp),
                ) {
                    groupedRecipients.forEach { (consonant, groupItems) ->
                        stickyHeader(key = "header_$consonant") {
                            ConsonantSectionHeader(consonant = consonant)
                        }
                        items(groupItems, key = { it.receiverId }) { recipient ->
                            RecipientListItem(
                                recipient = recipient,
                                selected = recipient.receiverId in selectedIds,
                                onSelectedChange = { checked ->
                                    if (checked) {
                                        selectedIds.add(recipient.receiverId)
                                    } else {
                                        selectedIds.remove(recipient.receiverId)
                                    }
                                },
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
    }
}

@Composable
private fun ConsonantSectionHeader(consonant: Char) {
    Text(
        text = consonant.toString(),
        style = AfternoteDesign.typography.captionLargeB,
        color = AfternoteDesign.colors.gray5,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun RecipientListScreenPrev() {
    RecipientListContent(
        recipients =
            listOf(
                ReceiverListItem(receiverId = 1L, name = "박경민", relation = "친구"),
                ReceiverListItem(receiverId = 2L, name = "김철수", relation = "가족"),
                ReceiverListItem(receiverId = 3L, name = "이영희", relation = "연인"),
            ),
        onBackClick = {},
        onConfirmClick = {},
    )
}
