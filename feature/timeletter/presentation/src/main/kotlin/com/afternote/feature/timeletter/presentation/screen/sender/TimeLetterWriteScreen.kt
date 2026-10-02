package com.afternote.feature.timeletter.presentation.screen.sender

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.compose.AsyncImage
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.calendar.BottomSheetCalendar
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.timeletter.domain.model.RecordedAudio
import com.afternote.feature.timeletter.presentation.R
import com.afternote.feature.timeletter.presentation.component.MediaBottomSheetContent
import com.afternote.feature.timeletter.presentation.component.RecipientCard
import com.afternote.feature.timeletter.presentation.component.SendScheduleRow
import com.afternote.feature.timeletter.presentation.component.TimeLetterBottomBar
import com.afternote.feature.timeletter.presentation.component.TimeLetterTextButton
import com.afternote.feature.timeletter.presentation.component.TimeLetterTitleTextField
import com.afternote.feature.timeletter.presentation.component.TimeWheelPicker
import com.afternote.feature.timeletter.presentation.viewmodel.EditorBlock
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteError
import com.afternote.feature.timeletter.presentation.viewmodel.TimeLetterWriteUiState
import com.afternote.feature.timeletter.presentation.viewmodel.VoiceRecordingState
import java.time.LocalDate
import java.time.LocalTime
import com.afternote.core.ui.R as CoreUiR

private enum class MicrophonePermissionError {
    Denied,
    PermanentlyDenied,
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeLetterWriteScreen(
    modifier: Modifier = Modifier,
    uiState: TimeLetterWriteUiState = TimeLetterWriteUiState(),
    titleState: TextFieldState = rememberTextFieldState(uiState.draftTitle.orEmpty()),
    onBackClick: () -> Unit = {},
    onRegisterClick: (title: String, textContents: Map<Long, String>) -> Unit = { _, _ -> },
    onRecipientClick: (title: String, textContents: Map<Long, String>) -> Unit = { _, _ -> },
    onTitleChanged: (String) -> Unit = {},
    onTextContentChanged: (blockId: Long, content: String) -> Unit = { _, _ -> },
    onDateSelected: (String) -> Unit = {},
    onTimeSelected: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    onDraftClick: (title: String, textContents: Map<Long, String>) -> Unit = { _, _ -> },
    onNavigateToDraft: () -> Unit = {},
    onErrorShown: () -> Unit = {},
    onAddImageBlock: (Uri) -> Unit = {},
    onAddAudioBlock: (Uri) -> Unit = {},
    onAddFileBlock: (Uri) -> Unit = {},
    onAddLinkBlock: (String) -> Unit = {},
    onRemoveBlock: (Long) -> Unit = {},
    onSetFocusedBlock: (Long?) -> Unit = {},
    onTextStyleClick: () -> Unit = {},
    onAlignCenterClick: () -> Unit = {},
    onAlignLeftClick: () -> Unit = {},
    onAlignRightClick: () -> Unit = {},
    onOpenVoiceRecorder: () -> Unit = {},
    onStartVoiceRecording: () -> Unit = {},
    onStopVoiceRecording: () -> Unit = {},
    onRegisterVoiceRecording: () -> Unit = {},
    onRetryVoiceRecording: () -> Unit = {},
    onDiscardVoiceRecording: () -> Unit = {},
    onFreePlanLimitConfirm: () -> Unit = {},
    onFreePlanLimitDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var showMediaSheet by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrlInput by remember { mutableStateOf("") }
    var microphonePermissionErrorType by remember { mutableStateOf<MicrophonePermissionError?>(null) }
    val microphonePermissionError = stringResource(R.string.timeletter_voice_recording_permission_error)
    val microphonePermissionSettings = stringResource(R.string.timeletter_voice_recording_permission_settings)
    val settingsActionLabel = stringResource(R.string.timeletter_voice_recording_permission_settings_action)
    val errorMessage = uiState.error?.message()

    val textBlockStates =
        remember(uiState.editingTimeLetterId) { androidx.compose.runtime.mutableStateMapOf<Long, TextFieldState>() }

    fun collectTextContents(): Map<Long, String> =
        collectTextBlockContents(
            editorBlocks = uiState.editorBlocks,
            visibleTextContents = textBlockStates.mapValues { (_, state) -> state.text.toString() },
            draftTextContents = uiState.draftTextContents,
        )

    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { onAddImageBlock(it) }
        }
    val audioLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { onAddAudioBlock(it) }
        }
    val recordAudioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                onStartVoiceRecording()
            } else {
                microphonePermissionErrorType =
                    if (
                        context.findActivity()?.let { activity ->
                            ActivityCompat.shouldShowRequestPermissionRationale(
                                activity,
                                Manifest.permission.RECORD_AUDIO,
                            )
                        } == false
                    ) {
                        MicrophonePermissionError.PermanentlyDenied
                    } else {
                        MicrophonePermissionError.Denied
                    }
            }
        }
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { onAddFileBlock(it) }
        }

    LaunchedEffect(uiState.error) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onErrorShown()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (
            uiState.voiceRecordingState is VoiceRecordingState.Starting ||
            uiState.voiceRecordingState is VoiceRecordingState.Recording
        ) {
            onDiscardVoiceRecording()
        }
    }

    LaunchedEffect(microphonePermissionErrorType) {
        val errorType = microphonePermissionErrorType ?: return@LaunchedEffect
        val result =
            snackbarHostState.showSnackbar(
                message =
                    when (errorType) {
                        MicrophonePermissionError.Denied -> microphonePermissionError
                        MicrophonePermissionError.PermanentlyDenied -> microphonePermissionSettings
                    },
                actionLabel =
                    if (errorType == MicrophonePermissionError.PermanentlyDenied) {
                        settingsActionLabel
                    } else {
                        null
                    },
            )
        if (result == SnackbarResult.ActionPerformed) {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        microphonePermissionErrorType = null
    }

    val currentOnTitleChanged by rememberUpdatedState(onTitleChanged)
    LaunchedEffect(titleState) {
        snapshotFlow { titleState.text.toString() }
            .collect { currentOnTitleChanged(it) }
    }

    if (uiState.isLoadingEditingLetter) {
        Scaffold(
            modifier = modifier,
            topBar = {
                DetailTopBar(
                    title = "",
                    onBackClick = onBackClick,
                )
            },
            containerColor = AfternoteDesign.colors.white,
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    // Apply the loaded title once per editing destination. Keying this to draftTitle would
    // overwrite the user's input whenever the draft is synchronized back to the ViewModel.
    LaunchedEffect(uiState.editingTimeLetterId) {
        if (uiState.editingTimeLetterId != null) {
            titleState.edit {
                replace(0, length, uiState.draftTitle.orEmpty())
            }
        }
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
                onTimeChange = { h, m ->
                    pendingHour = h
                    pendingMinute = m
                },
                modifier =
                    Modifier
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .fillMaxWidth(),
                initialHour = pendingHour,
                initialMinute = pendingMinute,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteButton(
                text = "확인",
                onClick = {
                    onTimeSelected(pendingHour, pendingMinute)
                    showTimePicker = false
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
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
                    onOpenVoiceRecorder()
                },
                onFileClick = {
                    showMediaSheet = false
                    fileLauncher.launch("application/pdf")
                },
                onLinkClick = {
                    showMediaSheet = false
                    linkUrlInput = ""
                    showLinkDialog = true
                },
            )
        }
    }

    if (uiState.showVoiceRecorder) {
        VoiceRecorderBottomSheet(
            state = uiState.voiceRecordingState,
            onDismiss = onDiscardVoiceRecording,
            onStart = {
                if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    onStartVoiceRecording()
                } else {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onPickAudioFile = {
                onDiscardVoiceRecording()
                audioLauncher.launch("audio/*")
            },
            onStop = onStopVoiceRecording,
            onRegister = onRegisterVoiceRecording,
            onRetry = onRetryVoiceRecording,
        )
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
                            onAddLinkBlock(url)
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

    if (uiState.showFreePlanLimitPopup) {
        Popup(
            type = PopupType.Variant2,
            message = "현재 플랜은 타임레터를 3건까지만 등록할 수 있습니다.\n구독 시, 더 많은 타임레터를 제한 없이\n남기고 관리할 수 있습니다.",
            onConfirm = onFreePlanLimitConfirm,
            onDismiss = onFreePlanLimitDismiss,
            confirmText = "구독 후 기록하기",
            dismissText = "나중에 하기",
            confirmButtonColor = AfternoteDesign.colors.gray3,
            dismissButtonColor = AfternoteDesign.colors.gray3,
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
                        text = if (uiState.editingTimeLetterId == null) "등록" else "수정",
                        onClick = {
                            onRegisterClick(
                                titleState.text.toString(),
                                collectTextContents(),
                            )
                        },
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
                onDraftClick = { onDraftClick(titleState.text.toString(), collectTextContents()) },
                onDraftCountClick = onNavigateToDraft,
            )
        },
        containerColor = AfternoteDesign.colors.white,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item(key = "recipient") {
                RecipientCard(
                    recipientName = uiState.recipientNames.joinToString(", "),
                    onClick = {
                        onRecipientClick(
                            titleState.text.toString(),
                            collectTextContents(),
                        )
                    },
                )
            }
            item(key = "divider_1") {
                HorizontalDivider(color = AfternoteDesign.colors.gray2, thickness = 1.dp)
            }
            item(key = "schedule") {
                SendScheduleRow(
                    date = uiState.sendAt ?: "",
                    time = uiState.sendTime ?: "",
                    onDateClick = { showDatePicker = true },
                    onTimeClick = { showTimePicker = true },
                )
            }
            item(key = "divider_2") {
                HorizontalDivider(color = AfternoteDesign.colors.gray2, thickness = 1.dp)
            }
            item(key = "title") {
                TimeLetterTitleTextField(state = titleState)
            }
            items(uiState.editorBlocks, key = { it.id }) { block ->
                when (block) {
                    is EditorBlock.Text -> {
                        TextBlockItem(
                            blockId = block.id,
                            textBlockStates = textBlockStates,
                            initialText = uiState.draftTextContents[block.id].orEmpty(),
                            textAlign = uiState.textAlign,
                            onFocused = { onSetFocusedBlock(block.id) },
                            onTextChanged = { content -> onTextContentChanged(block.id, content) },
                        )
                    }

                    is EditorBlock.Image -> {
                        ImageBlockItem(
                            uri = block.uri,
                            onRemove = { onRemoveBlock(block.id) },
                        )
                    }

                    is EditorBlock.Audio -> {
                        MediaBlockChip(
                            iconRes = CoreUiR.drawable.core_ui_ic_mic,
                            label = block.name,
                            onRemove = { onRemoveBlock(block.id) },
                        )
                    }

                    is EditorBlock.File -> {
                        MediaBlockChip(
                            iconRes = R.drawable.ic_file,
                            label = block.name,
                            onRemove = { onRemoveBlock(block.id) },
                        )
                    }

                    is EditorBlock.Link -> {
                        MediaBlockChip(
                            iconRes = com.afternote.core.ui.R.drawable.core_ui_ic_link,
                            label = block.url,
                            onRemove = { onRemoveBlock(block.id) },
                        )
                    }
                }
            }
        }
    }
}

internal fun collectTextBlockContents(
    editorBlocks: List<EditorBlock>,
    visibleTextContents: Map<Long, String>,
    draftTextContents: Map<Long, String>,
): Map<Long, String> =
    editorBlocks
        .filterIsInstance<EditorBlock.Text>()
        .associate { block ->
            block.id to (visibleTextContents[block.id] ?: draftTextContents[block.id].orEmpty())
        }

@Composable
private fun TimeLetterWriteError.message(): String =
    when (this) {
        TimeLetterWriteError.SendDateRequired -> stringResource(R.string.timeletter_write_send_date_required)
        TimeLetterWriteError.LoadFailed -> stringResource(R.string.timeletter_write_load_failed)
        TimeLetterWriteError.RecipientRequired -> stringResource(R.string.timeletter_write_recipient_required)
        TimeLetterWriteError.SaveFailed -> stringResource(R.string.timeletter_write_save_failed)
        TimeLetterWriteError.ServerRejection -> stringResource(R.string.timeletter_write_rejected)
        TimeLetterWriteError.VoiceRecordingStartFailed -> stringResource(R.string.timeletter_voice_recording_start_error)
        TimeLetterWriteError.VoiceRecordingStopFailed -> stringResource(R.string.timeletter_voice_recording_stop_error)
    }

@Composable
private fun TextBlockItem(
    blockId: Long,
    textBlockStates: SnapshotStateMap<Long, TextFieldState>,
    initialText: String,
    textAlign: TextAlign,
    onFocused: () -> Unit,
    onTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        remember(blockId) {
            textBlockStates.getOrPut(blockId) { TextFieldState(initialText) }
        }
    val currentOnTextChanged by rememberUpdatedState(onTextChanged)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { currentOnTextChanged(it) }
    }
    DisposableEffect(blockId) {
        onDispose { textBlockStates.remove(blockId) }
    }
    BasicTextField(
        state = state,
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onFocusChanged { if (it.isFocused) onFocused() },
        textStyle =
            AfternoteDesign.typography.bodySmallR.copy(
                color = AfternoteDesign.colors.gray9,
                textAlign = textAlign,
            ),
        cursorBrush = SolidColor(AfternoteDesign.colors.black),
        decorator = { innerTextField ->
            Box {
                if (state.text.isEmpty()) {
                    Text(
                        text = "내용을 입력하세요",
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray4,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ImageBlockItem(
    uri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceRecorderBottomSheet(
    state: VoiceRecordingState,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onPickAudioFile: () -> Unit,
    onStop: () -> Unit,
    onRegister: () -> Unit,
    onRetry: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
    ) {
        Text(
            text = stringResource(R.string.timeletter_voice_recording_title),
            style = AfternoteDesign.typography.h3,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (state) {
            VoiceRecordingState.Idle -> {
                Text(
                    text = stringResource(R.string.timeletter_voice_recording_instruction),
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray7,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(24.dp))
                AfternoteButton(
                    text = stringResource(R.string.timeletter_voice_recording_start),
                    onClick = onStart,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                )
                TextButton(
                    onClick = onPickAudioFile,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.timeletter_voice_recording_pick_file))
                }
            }

            VoiceRecordingState.Starting, VoiceRecordingState.Stopping -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is VoiceRecordingState.Recording -> {
                Text(
                    text = formatRecordingDuration(state.elapsedMillis),
                    style = AfternoteDesign.typography.h2,
                    color = AfternoteDesign.colors.gray9,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(modifier = Modifier.height(24.dp))
                AfternoteButton(
                    text = stringResource(R.string.timeletter_voice_recording_stop),
                    onClick = onStop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                )
            }

            is VoiceRecordingState.Recorded -> {
                RecordedVoiceControls(
                    state = state,
                    onRegister = onRegister,
                    onRetry = onRetry,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ColumnScope.RecordedVoiceControls(
    state: VoiceRecordingState.Recorded,
    onRegister: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    var isPlaying by remember(state.audio.uriString) { mutableStateOf(false) }
    var isPlayerReady by remember(state.audio.uriString) { mutableStateOf(false) }
    var playbackFailed by remember(state.audio.uriString) { mutableStateOf(false) }
    val player = remember(state.audio.uriString) { MediaPlayer() }

    DisposableEffect(player, state.audio.uriString) {
        player.setAudioAttributes(
            AudioAttributes
                .Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
        )
        player.setOnPreparedListener {
            isPlayerReady = true
            playbackFailed = false
        }
        player.setOnCompletionListener { isPlaying = false }
        player.setOnErrorListener { _, _, _ ->
            isPlaying = false
            isPlayerReady = false
            playbackFailed = true
            true
        }
        runCatching {
            player.setDataSource(context, Uri.parse(state.audio.uriString))
            player.prepareAsync()
        }.onFailure {
            playbackFailed = true
        }

        onDispose { player.release() }
    }

    Text(
        text =
            stringResource(
                R.string.timeletter_voice_recording_duration,
                formatRecordingDuration(state.audio.durationMillis),
            ),
        style = AfternoteDesign.typography.bodyBase,
        color = AfternoteDesign.colors.gray7,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(
        onClick = {
            runCatching {
                if (player.isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.start()
                    isPlaying = true
                }
            }.onFailure {
                isPlaying = false
                playbackFailed = true
            }
        },
        enabled = isPlayerReady,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Text(
            stringResource(
                if (isPlaying) {
                    R.string.timeletter_voice_recording_pause
                } else {
                    R.string.timeletter_voice_recording_play
                },
            ),
        )
    }
    if (playbackFailed) {
        Text(
            text = stringResource(R.string.timeletter_voice_recording_playback_error),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray7,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(
        onClick = onRetry,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) {
        Text(stringResource(R.string.timeletter_voice_recording_retry))
    }
    AfternoteButton(
        text = stringResource(R.string.timeletter_voice_recording_register),
        onClick = onRegister,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
    )
}

private fun formatRecordingDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun MediaBlockChip(
    iconRes: Int,
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(AfternoteDesign.colors.gray2, RoundedCornerShape(8.dp))
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AfternoteDesign.colors.gray7,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray7,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = AfternoteDesign.colors.gray5,
                modifier = Modifier.size(16.dp),
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

@Preview(name = "Voice recorder - idle", showBackground = true)
@Composable
private fun VoiceRecorderBottomSheetIdlePreview() {
    AfternoteTheme {
        VoiceRecorderBottomSheet(
            state = VoiceRecordingState.Idle,
            onDismiss = {},
            onStart = {},
            onPickAudioFile = {},
            onStop = {},
            onRegister = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Voice recorder - recording", showBackground = true)
@Composable
private fun VoiceRecorderBottomSheetRecordingPreview() {
    AfternoteTheme {
        VoiceRecorderBottomSheet(
            state = VoiceRecordingState.Recording(elapsedMillis = 65_000L),
            onDismiss = {},
            onStart = {},
            onPickAudioFile = {},
            onStop = {},
            onRegister = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Voice recorder - recorded", showBackground = true)
@Composable
private fun VoiceRecorderBottomSheetRecordedPreview() {
    AfternoteTheme {
        VoiceRecorderBottomSheet(
            state =
                VoiceRecordingState.Recorded(
                    audio =
                        RecordedAudio(
                            uriString = "content://preview/voice.m4a",
                            fileName = "voice.m4a",
                            mimeType = "audio/mp4",
                            durationMillis = 65_000L,
                        ),
                ),
            onDismiss = {},
            onStart = {},
            onPickAudioFile = {},
            onStop = {},
            onRegister = {},
            onRetry = {},
        )
    }
}
