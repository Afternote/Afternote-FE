package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.presentation.component.RecipientCard
import com.afternote.feature.timeletter.presentation.component.SendScheduleRow
import com.afternote.feature.timeletter.presentation.component.TimeLetterBodyTextField
import com.afternote.feature.timeletter.presentation.component.TimeLetterBottomBar
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton
import com.afternote.feature.timeletter.presentation.component.TimeLetterTitleTextField

data class TimeLetterWriteUiState(
    val recipientName: String = "박채연",
    val sendDate: String = "2026.03.22.",
    val sendTime: String = "09:22",
    val draftCount: Int = 1,
)

@Composable
fun TimeLetterWriteScreen(
    uiState: TimeLetterWriteUiState = TimeLetterWriteUiState(),
    titleState: TextFieldState = rememberTextFieldState(),
    bodyState: TextFieldState = rememberTextFieldState(),
    onBackClick: () -> Unit = {},
    onRegisterClick: () -> Unit = {},
    onRecipientClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onTimeClick: () -> Unit = {},
    onDraftClick: () -> Unit = {},
    onLinkClick: () -> Unit = {},
    onTextStyleClick: () -> Unit = {},
    onAlignCenterClick: () -> Unit = {},
    onAlignLeftClick: () -> Unit = {},
    onAlignRightClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "",
                onBackClick = onBackClick,
                actions = {
                    TimeLetterTextButton(
                        text = "등록",
                        onClick = onRegisterClick,
                    )
                },
            )
        },
        bottomBar = {
            TimeLetterBottomBar(
                draftCount = uiState.draftCount,
                onLinkClick = onLinkClick,
                onTextStyleClick = onTextStyleClick,
                onAlignCenterClick = onAlignCenterClick,
                onAlignLeftClick = onAlignLeftClick,
                onAlignRightClick = onAlignRightClick,
                onDraftClick = onDraftClick,
            )
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
        ) {
            RecipientCard(
                recipientName = uiState.recipientName,
                onClick = onRecipientClick,
            )

            HorizontalDivider(color = AfternoteDesign.colors.gray2, thickness = 1.dp)

            SendScheduleRow(
                date = uiState.sendDate,
                time = uiState.sendTime,
                onDateClick = onDateClick,
                onTimeClick = onTimeClick,
            )

            HorizontalDivider(color = AfternoteDesign.colors.gray2, thickness = 1.dp)

            TimeLetterTitleTextField(state = titleState)

            TimeLetterBodyTextField(
                state = bodyState,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TimeLetterWriteScreenPreview() {
    AfternoteTheme {
        TimeLetterWriteScreen()
    }
}
