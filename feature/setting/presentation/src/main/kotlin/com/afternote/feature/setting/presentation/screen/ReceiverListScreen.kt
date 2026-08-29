package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.common.util.KoreanConsonantUtil
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.KoreanConsonantIndex
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.ReceiverListItem
import kotlinx.coroutines.launch

@Composable
fun ReceiverListScreen(
    receivers: List<ReceiverListItem>,
    onBackClick: () -> Unit,
    onConfirmClick: (ReceiverListItem) -> Unit,
    onRegisterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedConsonant by remember { mutableStateOf<Char?>(null) }

    val groupedReceivers =
        remember(receivers, searchState.text) {
            val query = searchState.text.toString()
            val filtered =
                if (query.isBlank()) receivers else receivers.filter { it.name.contains(query) }
            KoreanConsonantUtil.groupByInitialConsonant(filtered) { it.name }
        }

    val consonantIndexMap =
        remember(groupedReceivers) {
            var index = 0
            buildMap {
                groupedReceivers.forEach { (consonant, items) ->
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
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(
                title = "수신자 목록",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            if (receivers.isNotEmpty()) {
                AfternoteButton(
                    text = "수신자 선택 완료하기",
                    onClick = {
                        receivers.find { it.receiverId == selectedId }?.let(onConfirmClick)
                    },
                    type = if (selectedId != null) AfternoteButtonType.Default else AfternoteButtonType.Un,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        },
    ) { innerPadding ->
        if (receivers.isEmpty()) {
            RecipientListEmptyContent(
                onRegisterClick = onRegisterClick,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(top = 10.dp),
            ) {
                AfternoteTextField(
                    state = searchState,
                    placeholder = stringResource(R.string.setting_receiver_search_placeholder),
                    type = TextFieldType.Search,
                    imeAction = ImeAction.Search,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                if (groupedReceivers.isEmpty()) {
                    ReceiverSearchEmptyContent(modifier = Modifier.fillMaxSize())
                } else {
                    Row(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(start = 20.dp),
                        ) {
                            groupedReceivers.forEach { (_, items) ->
                                items(items, key = { it.receiverId }) { receiver ->
                                    ReceiverListItem(
                                        receiver = receiver,
                                        selected = receiver.receiverId == selectedId,
                                        onSelectedChange = {
                                            selectedId =
                                                if (selectedId == receiver.receiverId) null else receiver.receiverId
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
    }
}

@Composable
private fun ReceiverSearchEmptyContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.setting_receiver_search_empty),
            style = AfternoteDesign.typography.bodyLargeR,
            color = AfternoteDesign.colors.gray8,
        )
    }
}

@Composable
private fun RecipientListEmptyContent(
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(105.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.setting_receiver_empty_title),
                    style = AfternoteDesign.typography.h1,
                    color = AfternoteDesign.colors.black,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.setting_receiver_empty_description),
                    style = AfternoteDesign.typography.h3,
                    color = AfternoteDesign.colors.gray6,
                )
            }
            Spacer(modifier = Modifier.height(56.dp))
            Image(
                painter = painterResource(R.drawable.ic_default_profile),
                contentDescription = null,
                modifier = Modifier.size(134.dp),
            )
        }
        Spacer(modifier = Modifier.height(56.dp))

        AfternoteButton(
            text = stringResource(R.string.setting_receiver_empty_register),
            onClick = onRegisterClick,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiverSearchEmptyPreview() {
    ReceiverSearchEmptyContent()
}

@Preview(showBackground = true)
@Composable
private fun ReceiverListEmptyPreview() {
    ReceiverListScreen(
        receivers = emptyList(),
        onBackClick = {},
        onConfirmClick = {},
        onRegisterClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ReceiverListScreenPrev() {
    ReceiverListScreen(
        receivers =
            listOf(
                ReceiverListItem(receiverId = 1L, name = "박경민", relation = "친구"),
                ReceiverListItem(receiverId = 2L, name = "김철수", relation = "가족"),
                ReceiverListItem(receiverId = 3L, name = "이영희", relation = "연인"),
            ),
        onBackClick = {},
        onConfirmClick = { _ -> },
    )
}
