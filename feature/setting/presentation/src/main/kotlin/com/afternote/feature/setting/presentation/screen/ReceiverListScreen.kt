package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.component.ReceiverListItem

@Composable
fun ReceiverListScreen(
    receivers: List<ReceiverListItem>,
    onBackClick: () -> Unit,
    onConfirmClick: (List<ReceiverListItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()
    val selectedIds = remember { mutableStateSetOf<Long>() }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "수신자 목록",
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            AfternoteButton(
                text = "수신자 선택 완료하기",
                onClick = {
                    onConfirmClick(receivers.filter { it.receiverId in selectedIds })
                },
                type = AfternoteButtonType.Default,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(top = 10.dp)
                    .padding(horizontal = 20.dp),
        ) {
            AfternoteTextField(
                state = searchState,
                placeholder = "Text Field",
                type = TextFieldType.Search,
                imeAction = ImeAction.Search,
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                items(receivers) { receiver ->
                    ReceiverListItem(
                        receiver = receiver,
                        selected = receiver.receiverId in selectedIds,
                        onSelectedChange = { checked ->
                            if (checked) {
                                selectedIds.add(receiver.receiverId)
                            } else {
                                selectedIds.remove(receiver.receiverId)
                            }
                        },
                    )
                }
                item { Spacer(modifier = Modifier.padding(14.dp)) }
            }
        }
    }
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
        onConfirmClick = {},
    )
}
