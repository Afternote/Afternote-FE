package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton

@Composable
fun DraftLetterScreen(
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onCompleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var hasSelection by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = "임시저장된 레터",
                onBackClick = onBackClick,
                actions = {
                    TimeLetterTextButton(
                        text = if (hasSelection) "완료" else "수정",
                        isActive = hasSelection,
                        onClick = if (hasSelection) onCompleteClick else onEditClick,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DraftLetterScreenPrev() {
    DraftLetterScreen()
}
