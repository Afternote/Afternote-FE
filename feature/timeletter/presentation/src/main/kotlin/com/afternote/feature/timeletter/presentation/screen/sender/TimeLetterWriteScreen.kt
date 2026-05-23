package com.afternote.feature.timeletter.presentation.screen.sender

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.afternote.feature.timeletter.presentation.component.AttachmentListSection
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
    uiState: TimeLetterWriteUiState = TimeLetterWriteUiState(),
    modifier: Modifier = Modifier,
    titleState: TextFieldState = rememberTextFieldState(),
    bodyState: TextFieldState = rememberTextFieldState(),
    onBackClick: () -> Unit = {},
    onRegisterClick: (title: String, body: String) -> Unit = { _, _ -> },
    onRecipientClick: () -> Unit = {},
    onDateSelected: (String) -> Unit = {},
    onTimeSelected: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onDraftClick: (title: String, body: String) -> Unit = { _, _ -> },
    onNavigateToDraft: () -> Unit = {},
    onErrorShown: () -> Unit = {},
    onImageSelected: (Uri) -> Unit = {},
    onAudioSelected: (Uri) -> Unit = {},
    onFileSelected: (Uri) -> Unit = {},
    onLinkAdded: (String) -> Unit = {},
    onAttachmentRemoved: (Int) -> Unit = {},
    onTextStyleClick: () -> Unit = {},
    onAlignCenterClick: () -> Unit = {},
    onAlignLeftClick: () -> Unit = {},
    onAlignRightClick: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var showMediaSheet by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrlInput by remember { mutableStateOf("") }

    val imageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { onImageSelected(it) }
        }

    val audioLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { onAudioSelected(it) }
        }

    val fileLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { onFileSelected(it) }
        }

    LaunchedEffect(uiState.errorMessage) {
        val msg = uiState.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onErrorShown()
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
                onDateSelected(date.toString())
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
                initialHour = pendingHour,
                initialMinute = pendingMinute,
                onTimeChanged = { h, m ->
                    pendingHour = h
                    pendingMinute = m
                },
                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally).fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteButton(
                text = "확인",
                onClick = {
                    onTimeSelected(pendingHour, pendingMinute)
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
                    imageLauncher.launch("image/*")
                },
                onVoiceClick = {
                    showMediaSheet = false
                    audioLauncher.launch("audio/*")
                },
                onFileClick = {
                    showMediaSheet = false
                    fileLauncher.launch("*/*")
                },
                onLinkClick = {
                    showMediaSheet = false
                    linkUrlInput = ""
                    showLinkDialog = true
                },
            )
        }
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = {
                Text(
                    text = "링크 추가하기",
                    style = AfternoteDesign.typography.h3,
                    color = AfternoteDesign.colors.gray9,
                )
            },
            text = {
                TextField(
                    value = linkUrlInput,
                    onValueChange = { linkUrlInput = it },
                    placeholder = {
                        Text(
                            text = "URL을 입력하세요",
                            style = AfternoteDesign.typography.bodySmallR,
                            color = AfternoteDesign.colors.gray4,
                        )
                    },
                    singleLine = true,
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = AfternoteDesign.colors.gray2,
                            unfocusedContainerColor = AfternoteDesign.colors.gray2,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = linkUrlInput.trim()
                        if (url.isNotBlank()) {
                            onLinkAdded(url)
                            showLinkDialog = false
                        }
                    },
                ) {
                    Text("추가", color = AfternoteDesign.colors.gray9)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text("취소", color = AfternoteDesign.colors.gray7)
                }
            },
            containerColor = AfternoteDesign.colors.white,
        )
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
                onDraftCountClick = onNavigateToDraft,
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
                textAlign = uiState.textAlign,
                modifier = Modifier.weight(1f, fill = false),
            )

            if (uiState.attachments.isNotEmpty()) {
                AttachmentListSection(
                    attachments = uiState.attachments,
                    onRemove = onAttachmentRemoved,
                )
            }
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
