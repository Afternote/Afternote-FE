package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.R
import com.afternote.core.ui.asString
import com.afternote.core.ui.calendar.BottomSheetCalendar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.component.ReceiverSelectBottomSheet
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import java.time.format.DateTimeFormatter
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

private val WriteDateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

@Composable
fun DiaryWriteScreen(
    modifier: Modifier = Modifier,
    onSubmitSuccess: () -> Unit,
    onBackClick: () -> Unit,
    onDraftListClick: () -> Unit,
    viewModel: DiaryWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showReceiverSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    DiaryWriteScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onSubmit = { viewModel.submit() },
        onSaveDraft = { viewModel.submit(isDraft = true) },
        onDraftListClick = onDraftListClick,
        onTitleChanged = viewModel::onTitleChanged,
        onContentChanged = viewModel::onContentChanged,
        onMoodSelected = viewModel::onMoodSelected,
        onReceiverRowClick = { showReceiverSheet = true },
        onDateRowClick = { showDateSheet = true },
        onMediaPicked = viewModel::uploadMedia,
    )

    if (showReceiverSheet) {
        ReceiverSelectBottomSheet(
            receivers = uiState.receivers,
            selectedReceiverIds = uiState.selectedReceiverIds,
            // 실패를 빈 목록으로 흡수하지 않는다 — 사용자가 «등록 안 함» 으로 오해한다 (#1019).
            loadError = uiState.receiverLoadError?.asString(),
            isLoading = uiState.isReceiverLoading,
            onRetry = viewModel::loadReceivers,
            onToggle = viewModel::onReceiverToggled,
            onDismiss = { showReceiverSheet = false },
        )
    }

    if (showDateSheet) {
        BottomSheetCalendar(
            title = stringResource(MindRecordR.string.mindrecord_write_date_picker_title),
            initialDate = uiState.date,
            // 미래 날짜는 서버가 400(code 2101)으로 거절한다. 시트는 core:ui 공용이라
            // 달력에서 막지 못하므로 ViewModel 이 받아서 사유와 함께 되돌린다 (#1008).
            onDateSelect = {
                viewModel.onDateSelected(it)
                showDateSheet = false
            },
            onDismiss = { showDateSheet = false },
        )
    }
}

/**
 * ViewModel 과 분리된 일기 작성 화면 본문 (#1359).
 *
 * screenshotTest 가 고정 상태를 그대로 렌더할 수 있도록 상태는 [uiState] 하나로 받고
 * 이벤트는 콜백으로 받는다. 수신자 바텀시트와 제출 성공 신호는 **래퍼가 소유한다** —
 * 시트는 자체 표시 상태를 들고 있어 baseline 을 흔들고, 성공 신호는 화면 밖 이동이라
 * 렌더와 무관하다.
 *
 * 콜백 기본값을 두는 것은 이 화면을 상태만으로 렌더하는 자리(프리뷰·screenshotTest)를
 * 위해서다. 프로덕션 호출부는 래퍼 하나뿐이라 누락이 생기지 않는다.
 */
@Composable
internal fun DiaryWriteScreenContent(
    uiState: DiaryWriteUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit,
    onSaveDraft: () -> Unit,
    onDraftListClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onContentChanged: (String) -> Unit,
    onMoodSelected: (TodayMood) -> Unit,
    onReceiverRowClick: () -> Unit,
    onDateRowClick: () -> Unit,
    onMediaPicked: suspend (String) -> String? = { null },
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(MindRecordR.string.mindrecord_diary_write_title),
                onBackClick = onBackClick,
                actions = {
                    // 완성되지 않아도 **누를 수는 있게** 둔다. 비활성이면 submit() 이 아예
                    // 돌지 않아 무엇이 빠졌는지 알릴 자리가 없다 — 회색 버튼만으로는 고장과
                    // 구분되지 않는다 (#722). 색은 종전대로 미완성일 때 흐리게 둔다.
                    Button(
                        onClick = onSubmit,
                        // 완성되지 않아도 **누를 수는 있게** 둔다 — `canSubmit` 이 아닌 이유다.
                        // 비활성이면 submit() 이 아예 돌지 않아 무엇이 빠졌는지 알릴 자리가 없고,
                        // 회색 버튼만으로는 고장과 구분되지 않는다 (#722). 색은 아래에서 미완성일
                        // 때 흐리게 둔다.
                        enabled = uiState.submitState != SubmitState.InProgress,
                        shape = RoundedCornerShape(6.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (uiState.canSubmit) {
                                        AfternoteDesign.colors.gray9
                                    } else {
                                        AfternoteDesign.colors.gray2
                                    },
                                contentColor =
                                    if (uiState.canSubmit) {
                                        AfternoteDesign.colors.white
                                    } else {
                                        AfternoteDesign.colors.gray6
                                    },
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
                        .clickable(role = Role.Button, onClick = onReceiverRowClick)
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
            //
            // 서버가 2026-08-29 부터 생성·수정 양쪽에서 `date` 를 받는다 (Afternote-BE#244, PR #262).
            // 그 전까지는 고른 날짜가 요청에 실리지 않아 «고를 수 있지만 반영되지 않는» 상태였고,
            // #1121 이 그 동안 이 행을 표시 전용으로 잠가 뒀다. 계약이 왔으므로 시안대로 되돌린다 (#1008).
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onDateRowClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = uiState.date.format(WriteDateFormatter),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray9,
                )
                Icon(
                    painter = painterResource(R.drawable.core_ui_arrowdown),
                    contentDescription = null,
                    tint = AfternoteDesign.colors.gray9,
                    modifier = Modifier.size(16.dp),
                )
            }

            HorizontalDivider()

            TextField(
                value = uiState.title,
                onValueChange = onTitleChanged,
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

            // 기분은 «셋 중 하나» 다. 정렬 아이콘과 같은 처방으로 selectableGroup 안의
            // Role.RadioButton 으로 읽히게 한다 — 종전에는 맨 clickable 이라 역할도 선택
            // 상태도 실리지 않았다 (#1179).
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
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

                MoodChip(
                    mood = TodayMood.HAPPY,
                    selected = uiState.mood == TodayMood.HAPPY,
                    onClick = { onMoodSelected(TodayMood.HAPPY) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                MoodChip(
                    mood = TodayMood.SOSO,
                    selected = uiState.mood == TodayMood.SOSO,
                    onClick = { onMoodSelected(TodayMood.SOSO) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                MoodChip(
                    mood = TodayMood.SAD,
                    selected = uiState.mood == TodayMood.SAD,
                    onClick = { onMoodSelected(TodayMood.SAD) },
                )
            }

            val errorMessage =
                (uiState.submitState as? SubmitState.Failed)?.message?.asString()
                    ?: uiState.dateError?.asString()
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
                onValueChange = onContentChanged,
                onSaveDraftClick = onSaveDraft,
                onDraftCountClick = onDraftListClick,
                draftCount = uiState.draftCount,
                onImagePicked = onMediaPicked,
                onMediaPicked = onMediaPicked,
            )
        }
    }
}

/**
 * 기분 하나를 고르는 칩.
 *
 * 이름은 이모지가 아니라 [TodayMood.label] 의 낱말이다 — 이모지만 실으면 스크린리더가
 * "우는 얼굴, 버튼" 처럼 그림을 묘사할 뿐 «슬픔을 고른다» 를 말하지 못한다 (#1179 리뷰).
 * 선택 여부는 색으로만 구분되던 것을 [Modifier.selectable] 의 selected 로도 싣는다.
 */
@Composable
private fun MoodChip(
    mood: TodayMood,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(mood.labelRes())
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
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = mood.emoji())
    }
}

@StringRes
private fun TodayMood.labelRes(): Int =
    when (this) {
        TodayMood.HAPPY -> MindRecordR.string.mindrecord_diary_write_mood_happy
        TodayMood.SOSO -> MindRecordR.string.mindrecord_diary_write_mood_soso
        TodayMood.SAD -> MindRecordR.string.mindrecord_diary_write_mood_sad
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
