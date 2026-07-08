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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionRecommendCard
import com.afternote.feature.mindrecord.presentation.component.RecipientSelectBottomSheet
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.afternote.core.ui.R as CoreUiR

@Composable
fun DailyQuestionWriteScreen(
    modifier: Modifier = Modifier,
    onSubmitSuccess: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onDraftListClick: () -> Unit = {},
    viewModel: DailyQuestionWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    // 백엔드 페이로드에 제목/기분 필드가 아직 없어 화면 로컬 상태로만 유지한다.
    var title by rememberSaveable { mutableStateOf("") }
    var selectedMood by rememberSaveable { mutableStateOf<TodayMood?>(null) }
    var isQuestionExpanded by rememberSaveable { mutableStateOf(true) }
    var showRecipientSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    val today = remember { LocalDate.now() }
    val topBarDateText = remember(today) { today.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) }
    val koreanDateText = remember(today) { today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일")) }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = topBarDateText,
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
                                disabledContentColor = AfternoteDesign.colors.gray5,
                            ),
                    ) {
                        Text(
                            text = stringResource(R.string.mindrecord_action_save),
                            style = AfternoteDesign.typography.bodySmallB,
                        )
                    }
                },
                onBackClick = onBackClick,
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
                DateSelectorRow(dateText = koreanDateText)

                TitleInputField(
                    title = title,
                    onTitleChange = { title = it },
                )

                MoodSelectorRow(
                    selectedMood = selectedMood,
                    onMoodSelect = { selectedMood = it },
                )

                val questionLoadErrorText = uiState.questionLoadError?.asString()
                val questionText =
                    when {
                        uiState.isQuestionLoading -> stringResource(R.string.mindrecord_daily_question_write_loading)
                        questionLoadErrorText != null -> questionLoadErrorText
                        uiState.questionContent.isNotEmpty() -> uiState.questionContent
                        else -> stringResource(R.string.mindrecord_daily_question_write_none)
                    }
                DailyQuestionRecommendCard(
                    questionText = questionText,
                    dayCount = uiState.questionDay,
                    expanded = isQuestionExpanded,
                    onExpandToggle = { isQuestionExpanded = !isQuestionExpanded },
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
                value = uiState.answer,
                onValueChange = viewModel::onAnswerChanged,
                onSaveDraftClick = { viewModel.submit(isDraft = true) },
                onDraftCountClick = onDraftListClick,
                contentPadding = PaddingValues(horizontal = 20.dp),
                bottomContent = {
                    RecipientSection(
                        onClick = { showRecipientSheet = true },
                        recipientName = recipientLabel(uiState.selectedReceiverNames),
                    )
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

/** 상단 날짜 표시 행. Figma 2372:22352 — 날짜 텍스트 + 화살표 + 하단 구분선. */
@Composable
private fun DateSelectorRow(
    dateText: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
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

/** 제목 입력 필드. Figma 2372:22353 — H3 스타일, 20% 블랙 플레이스홀더. */
@Composable
private fun TitleInputField(
    title: String,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = AfternoteDesign.typography.h3.copy(color = AfternoteDesign.colors.gray9),
        singleLine = true,
        cursorBrush = SolidColor(AfternoteDesign.colors.gray9),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (title.isEmpty()) {
                    Text(
                        text = stringResource(R.string.mindrecord_diary_write_title_placeholder),
                        style = AfternoteDesign.typography.h3,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.2f),
                    )
                }
                innerTextField()
            }
        },
    )
}

/** 오늘의 기분 이모지 선택 행. Figma 2372:22356. */
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
                painter = painterResource(R.drawable.mindrecord_emotion),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray6,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.mindrecord_diary_write_mood_label),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(TodayMood.HAPPY, TodayMood.SOSO, TodayMood.SAD).forEach { mood ->
                DailyMoodChip(
                    emoji = mood.moodEmoji(),
                    selected = selectedMood == mood,
                    onClick = { onMoodSelect(mood) },
                )
            }
        }
    }
}

@Composable
private fun DailyMoodChip(
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

/** 수신자 설정 영역. Figma 2372:22799 — 안내 문구 + 수신자 설정 진입 행. */
@Composable
private fun RecipientSection(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    recipientName: String? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "오늘의 기록을 전달하고 싶은 사람이 있나요?",
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray7,
        )
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_user),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray6,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = recipientName?.let { "$it 님에게" } ?: "수신자 설정하기",
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
}

private fun TodayMood.moodEmoji(): String =
    when (this) {
        TodayMood.HAPPY -> "😊"
        TodayMood.SOSO -> "😐"
        TodayMood.SAD -> "😢"
    }

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteScreenPreview() {
    AfternoteTheme {
        // Preview는 ViewModel 없이 호출 불가 — 컴파일 확인용 placeholder
    }
}
