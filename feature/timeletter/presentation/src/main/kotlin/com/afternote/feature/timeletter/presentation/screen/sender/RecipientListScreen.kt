package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.domain.Recipient
import com.afternote.feature.timeletter.presentation.component.RecipientListItem

@Composable
fun RecipientListScreen(
    recipients: List<Recipient>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchState = rememberTextFieldState()

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "수신인 선택",
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            AfternoteTextField(
                state = searchState,
                placeholder = "이름으로 검색",
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray9,
                        modifier = Modifier.size(24.dp),
                    )
                },
                imeAction = ImeAction.Search,
            )
            LazyColumn(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                items(recipients) { recipient ->
                    RecipientListItem(recipient = recipient)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipientListScreenPrev() {
    RecipientListScreen(
        recipients = listOf(
            Recipient(id = 1L, name = "박경민", relationship = "친구"),
            Recipient(id = 2L, name = "김철수", relationship = "가족"),
            Recipient(id = 3L, name = "이영희", relationship = "연인"),
        ),
        onBackClick = {},
    )
}
