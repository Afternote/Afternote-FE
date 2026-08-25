package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.R
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.component.BottomSheetCalendar
import com.afternote.feature.mindrecord.presentation.component.ReceiverSelectBottomSheet
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

private val WriteDateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

@Composable
fun DiaryWriteScreen(
    modifier: Modifier = Modifier,
    onSubmitSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDraftListClick: () -> Unit = {},
    viewModel: DiaryWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var showReceiverSheet by remember { mutableStateOf(false) }
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(MindRecordR.string.mindrecord_diary_write_title),
                onBackClick = onBackClick,
                actions = {
                    Button(
                        onClick = { viewModel.submit() },
                        enabled = uiState.canSubmit,
                        shape = RoundedCornerShape(6.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AfternoteDesign.colors.gray9,
                                contentColor = AfternoteDesign.colors.white,
                                disabledContainerColor = AfternoteDesign.colors.gray2,
                                disabledContentColor = AfternoteDesign.colors.gray6,
                            ),
                    ) {
                        Text(
                            text = stringResource(MindRecordR.string.mindrecord_action_register),
                            style = AfternoteDesign.typography.bodySmallB,
                        )
                    }
                },
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
        ) {
            // Figma 2671:17921 — 수신자 행: 아바타 + "OO님에게" / 미설정 시 "수신자 설정하기"
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showReceiverSheet = true }
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AfternoteDesign.colors.gray2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_user),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray6,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val selectedReceivers = uiState.selectedReceivers
                Text(
                    text =
                        when {
                            selectedReceivers.isEmpty() -> {
                                stringResource(MindRecordR.string.mindrecord_write_receiver_setting)
                            }

                            selectedReceivers.size == 1 -> {
                                stringResource(MindRecordR.string.mindrecord_write_receiver_to, selectedReceivers.first().name)
                            }

                            else -> {
                                stringResource(
                                    MindRecordR.string.mindrecord_write_receiver_to_multiple,
                                    selectedReceivers.first().name,
                                    selectedReceivers.size - 1,
                                )
                            }
                        },
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.core_ui_right),
                    contentDescription = null,
                    tint = AfternoteDesign.colors.gray9,
                    modifier = Modifier.size(16.dp),
                )
            }

            // Figma 2671:17922 — 날짜 선택 행 ("yyyy년 M월 d일" + 아래 화살표)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = !showPicker },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.date.format(WriteDateFormatter),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                IconButton(onClick = { showPicker = !showPicker }) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_arrowdown),
                        contentDescription = null,
                    )
                }
            }

            HorizontalDivider()

            TextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                textStyle = AfternoteDesign.typography.h3,
                placeholder = {
                    Text(
                        text = stringResource(MindRecordR.string.mindrecord_diary_write_title_placeholder),
                        style = AfternoteDesign.typography.h3,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.2f),
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val selectedMood = uiState.mood
                if (selectedMood != null) {
                    Text(text = selectedMood.emoji())
                } else {
                    Icon(
                        painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_emotion),
                        contentDescription = null,
                        tint = Color(0xFF000000).copy(0.4f),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(MindRecordR.string.mindrecord_diary_write_mood_label),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
                )
                Spacer(modifier = Modifier.width(12.dp))

                MoodChip("😊", selected = uiState.mood == TodayMood.HAPPY) {
                    viewModel.onMoodSelected(TodayMood.HAPPY)
                }
                Spacer(modifier = Modifier.width(8.dp))
                MoodChip("😐", selected = uiState.mood == TodayMood.SOSO) {
                    viewModel.onMoodSelected(TodayMood.SOSO)
                }
                Spacer(modifier = Modifier.width(8.dp))
                MoodChip("😢", selected = uiState.mood == TodayMood.SAD) {
                    viewModel.onMoodSelected(TodayMood.SAD)
                }
            }

            val errorMessage =
                (uiState.submitState as? SubmitState.Failed)?.message?.asString()
                    ?: uiState.draftLoadError?.asString()
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = AfternoteDesign.colors.error,
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            // 저장·업로드 진행 상태를 알린다 — 종전에는 액션만 잠기고 표시가 없어
            // 사용자가 무반응으로 인식했다 (#716).
            val progressText =
                when {
                    uiState.submitState is SubmitState.InProgress -> {
                        stringResource(MindRecordR.string.mindrecord_write_saving)
                    }

                    uiState.isUploadingImage -> {
                        stringResource(MindRecordR.string.mindrecord_write_uploading_image)
                    }

                    uiState.isDraftLoading -> {
                        stringResource(MindRecordR.string.mindrecord_write_loading_draft)
                    }

                    else -> {
                        null
                    }
                }
            if (progressText != null) {
                Text(
                    text = progressText,
                    color = AfternoteDesign.colors.gray6,
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            val uploadError = uiState.imageUploadError?.asString()
            if (uploadError != null) {
                Text(
                    text = uploadError,
                    color = AfternoteDesign.colors.error,
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            // 프리필이 늦게 도착해도 WriteTextField 가 value 변경에 반응해 다시 시드한다 —
            // key() 로 컴포넌트를 재생성하지 않는다 (#1018).
            WriteTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChanged,
                onSaveDraftClick = { viewModel.submit(isDraft = true) },
                onDraftCountClick = onDraftListClick,
                draftCount = uiState.draftCount,
                onImagePicked = viewModel::uploadImage,
            )
        }

        if (showPicker) {
            BottomSheetCalendar(
                initialDate = uiState.date,
                onDismiss = { showPicker = false },
                onDateSelect = { date ->
                    viewModel.onDateSelected(date)
                    showPicker = false
                },
            )
        }

        if (showReceiverSheet) {
            ReceiverSelectBottomSheet(
                receivers = uiState.receivers,
                selectedReceiverIds = uiState.selectedReceiverIds,
                onToggle = viewModel::onReceiverToggled,
                onDismiss = { showReceiverSheet = false },
            )
        }
    }
}

@Composable
private fun MoodChip(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(
                    if (selected) {
                        AfternoteDesign.colors.gray2
                    } else {
                        Color(0xFF000000).copy(0.05f)
                    },
                ).size(32.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji)
    }
}

private fun TodayMood.emoji(): String =
    when (this) {
        TodayMood.HAPPY -> "😊"
        TodayMood.SOSO -> "😐"
        TodayMood.SAD -> "😢"
    }

@Preview(showBackground = true)
@Composable
private fun DiaryWriteScreenPreview() {
    AfternoteTheme {
        // ViewModel 의존이 있어 Preview는 비워둠
    }
}
