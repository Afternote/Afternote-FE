package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.calendar.BottomSheetCalendar
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
import com.afternote.feature.timeletter.presentation.component.TimeWheelPicker
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteUiState
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeLetterWriteScreen(
    modifier: Modifier = Modifier,
    uiState: TimeLetterWriteUiState = TimeLetterWriteUiState(),
    titleState: TextFieldState = rememberTextFieldState(),
    bodyState: TextFieldState = rememberTextFieldState(),
    onBackClick: () -> Unit = {},
    onRegisterClick: (title: String, body: String) -> Unit = { _, _ -> },
    onRecipientClick: () -> Unit = {},
    onDateSelect: (String) -> Unit = {},
    onTimeSelect: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onDraftClick: (title: String, body: String) -> Unit = { _, _ -> },
    onErrorShow: () -> Unit = {},
    onMediaImageClick: () -> Unit = {},
    onMediaVoiceClick: () -> Unit = {},
    onMediaFileClick: () -> Unit = {},
    onMediaLinkClick: () -> Unit = {},
    onTextStyleClick: () -> Unit = {},
    onAlignCenterClick: () -> Unit = {},
    onAlignLeftClick: () -> Unit = {},
    onAlignRightClick: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var showMediaSheet by remember { mutableStateOf(false) }
    val currentOnErrorShow by rememberUpdatedState(onErrorShow)

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        currentOnErrorShow()
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingHour by remember { mutableIntStateOf(LocalTime.now().hour) }
    var pendingMinute by remember { mutableIntStateOf(LocalTime.now().minute) }

    if (showDatePicker) {
        val initialDate =
            uiState.sendAt
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()
        BottomSheetCalendar(
            title = "발송 날짜",
            initialDate = initialDate,
            onDismiss = { showDatePicker = false },
            onDateSelect = { date ->
                onDateSelect(date.toString())
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        ModalBottomSheet(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color.White,
        ) {
            Text(
                text = "발송 시간",
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            TimeWheelPicker(
                onTimeChange = { h, m ->
                    pendingHour = h
                    pendingMinute = m
                },
                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally).fillMaxWidth(),
                initialHour = pendingHour,
                initialMinute = pendingMinute,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteButton(
                text = "확인",
                onClick = {
                    onTimeSelect(pendingHour, pendingMinute)
                    showTimePicker = false
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showMediaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
        ) {
            MediaBottomSheetContent(
                onImageClick = {
                    showMediaSheet = false
                    onMediaImageClick()
                },
                onVoiceClick = {
                    showMediaSheet = false
                    onMediaVoiceClick()
                },
                onFileClick = {
                    showMediaSheet = false
                    onMediaFileClick()
                },
                onLinkClick = {
                    showMediaSheet = false
                    onMediaLinkClick()
                },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = "타임레터",
                onBackClick = onBackClick,
                actions = {
                    TimeLetterTextButton(
                        text = "등록",
                        onClick = { onRegisterClick(titleState.text.toString(), bodyState.text.toString()) },
                        isActive = !uiState.isSaving && uiState.sendAt != null,
                    )
                },
            )
        },
        bottomBar = {
            TimeLetterBottomBar(
                draftCount = uiState.draftCount,
                textAlign = uiState.textAlign,
                onMediaAddClick = { showMediaSheet = true },
                onTextStyleClick = onTextStyleClick,
                onAlignCenterClick = onAlignCenterClick,
                onAlignLeftClick = onAlignLeftClick,
                onAlignRightClick = onAlignRightClick,
                onDraftClick = { onDraftClick(titleState.text.toString(), bodyState.text.toString()) },
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
                recipientName = uiState.recipientNames.joinToString(", "),
                onClick = onRecipientClick,
            )

            HorizontalDivider(color = AfternoteDesign.colors.gray2, thickness = 1.dp)

            SendScheduleRow(
                date = uiState.sendAt ?: "",
                time = uiState.sendTime ?: "",
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
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
