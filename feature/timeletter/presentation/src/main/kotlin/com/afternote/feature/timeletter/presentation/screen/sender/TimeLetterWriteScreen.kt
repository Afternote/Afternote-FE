package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.presentation.component.MediaBottomSheetContent
import com.afternote.feature.timeletter.presentation.component.RecipientCard
import com.afternote.feature.timeletter.presentation.component.SendScheduleRow
import com.afternote.feature.timeletter.presentation.component.TimeLetterBodyTextField
import com.afternote.feature.timeletter.presentation.component.TimeLetterBottomBar
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton
import com.afternote.feature.timeletter.presentation.component.TimeLetterTitleTextField
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteUiState

@OptIn(ExperimentalMaterial3Api::class)
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
    onMediaImageClick: () -> Unit = {},
    onMediaVoiceClick: () -> Unit = {},
    onMediaFileClick: () -> Unit = {},
    onMediaLinkClick: () -> Unit = {},
    onLinkClick: () -> Unit = {},
    onTextStyleClick: () -> Unit = {},
    onAlignCenterClick: () -> Unit = {},
    onAlignLeftClick: () -> Unit = {},
    onAlignRightClick: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState()
    var showMediaSheet by remember { mutableStateOf(false) }

    if (showMediaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            MediaBottomSheetContent(
                onImageClick = { showMediaSheet = false; onMediaImageClick() },
                onVoiceClick = { showMediaSheet = false; onMediaVoiceClick() },
                onFileClick = { showMediaSheet = false; onMediaFileClick() },
                onLinkClick = { showMediaSheet = false; onMediaLinkClick() },
            )
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "타임레터",
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
                onMediaAddClick = { showMediaSheet = true },
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

@Preview(showBackground = true)
@Composable
private fun TimeLetterWriteScreenPreview() {
    AfternoteTheme {
        TimeLetterWriteScreen()
    }
}
