package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Red
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.component.BottomSheetCalendar
import com.afternote.feature.mindrecord.presentation.component.RecipientSelectBottomSheet
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import java.time.format.DateTimeFormatter
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

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
    var showRecipientSheet by remember { mutableStateOf(false) }
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    val dateText =
        remember(uiState.date) {
            uiState.date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일"))
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
                                containerColor = AfternoteDesign.colors.gray2,
                                contentColor = AfternoteDesign.colors.gray6,
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
        containerColor = AfternoteDesign.colors.gray1,
        modifier = modifier,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecipientRow(
                    onClick = { showRecipientSheet = true },
                    recipientName = recipientLabel(uiState.selectedReceiverNames),
                )

                DateSelectorRow(
                    dateText = dateText,
                    onClick = { showPicker = true },
                )

                TitleInputField(
                    title = uiState.title,
                    onTitleChange = viewModel::onTitleChanged,
                )

                MoodSelectorRow(
                    selectedMood = uiState.mood,
                    onMoodSelect = viewModel::onMoodSelected,
                )

                val errorMessage = (uiState.submitState as? SubmitState.Failed)?.message?.asString()
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Red,
                        style = AfternoteDesign.typography.captionLargeR,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            WriteTextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChanged,
                onSaveDraftClick = { viewModel.submit(isDraft = true) },
                onDraftCountClick = onDraftListClick,
                contentPadding = PaddingValues(horizontal = 20.dp),
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

        if (showRecipientSheet) {
            RecipientSelectBottomSheet(
                receivers = uiState.receivers,
                selectedIds = uiState.selectedReceiverIds,
                onConfirm = { ids ->
                    viewModel.onReceiversSelected(ids)
                    showRecipientSheet = false
                },
                onDismiss = { showRecipientSheet = false },
            )
        }
    }
}

/** 선택된 수신자 표기: 1명이면 이름, 여러 명이면 "OO 외 N명". */
private fun recipientLabel(names: List<String>): String? =
    when {
        names.isEmpty() -> null
        names.size == 1 -> names.first()
        else -> "${names.first()} 외 ${names.size - 1}명"
    }

/** 수신자 표시 행. Figma 2671:17923 — 아바타 + `OO님에게` + chevron. */
@Composable
private fun RecipientRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    recipientName: String? = null,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.core_ui_user),
            contentDescription = null,
            tint = AfternoteDesign.colors.gray6,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = recipientName?.let { "${it}님에게" } ?: "수신자 설정하기",
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
        Icon(
            painter = painterResource(CoreUiR.drawable.core_ui_right),
            contentDescription = null,
            tint = AfternoteDesign.colors.gray9,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 발송 날짜 선택 행. Figma 2671:17930 — 날짜 텍스트 + 아래 화살표 + 하단 0.5dp 구분선. */
@Composable
private fun DateSelectorRow(
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateText,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
            )
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_arrowdown),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray9,
                modifier = Modifier.size(16.dp),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = AfternoteDesign.colors.gray3)
    }
}

/** 제목 입력 필드. Figma 2671:17931 — H2 스타일, 20% 블랙 플레이스홀더. */
@Composable
private fun TitleInputField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = AfternoteDesign.typography.h2.copy(color = AfternoteDesign.colors.gray9),
        singleLine = true,
        cursorBrush = SolidColor(AfternoteDesign.colors.gray9),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(MindRecordR.string.mindrecord_diary_write_title_placeholder),
                        style = AfternoteDesign.typography.h2,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.2f),
                    )
                }
                innerTextField()
            }
        },
    )
}

/** 오늘의 기분 이모지 선택 행. Figma 2671:18323. */
@Composable
private fun MoodSelectorRow(
    selectedMood: TodayMood?,
    onMoodSelect: (TodayMood) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(MindRecordR.drawable.mindrecord_emotion),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray6,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(MindRecordR.string.mindrecord_diary_write_mood_label),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(TodayMood.HAPPY, TodayMood.SOSO, TodayMood.SAD).forEach { mood ->
                MoodChip(
                    emoji = mood.emoji(),
                    selected = selectedMood == mood,
                    onClick = { onMoodSelect(mood) },
                )
            }
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
                .size(32.dp)
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray2)
                .then(
                    if (selected) {
                        Modifier.border(1.dp, AfternoteDesign.colors.gray9, CircleShape)
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick),
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
