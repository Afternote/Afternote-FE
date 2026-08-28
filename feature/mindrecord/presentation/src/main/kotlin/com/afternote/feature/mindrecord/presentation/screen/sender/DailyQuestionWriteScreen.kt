package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionBanner
import com.afternote.feature.mindrecord.presentation.component.WriteTextField
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionWriteViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.SubmitState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val TopBarDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

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
    var questionExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    DailyQuestionWriteScreenContent(
        uiState = uiState,
        date = LocalDate.now(),
        questionExpanded = questionExpanded,
        modifier = modifier,
        onSubmitClick = { viewModel.submit() },
        onBackClick = onBackClick,
        onQuestionToggle = { questionExpanded = !questionExpanded },
        onAnswerChanged = viewModel::onAnswerChanged,
        onSaveDraftClick = { viewModel.submit(isDraft = true) },
        onDraftListClick = onDraftListClick,
        onMediaPicked = viewModel::uploadMedia,
    )
}

/** ViewModel·현재 시각 없이 같은 작성 상태를 Preview·screenshotTest 에 고정하는 본문 경계 (#1131). */
@Composable
internal fun DailyQuestionWriteScreenContent(
    uiState: DailyQuestionWriteUiState,
    date: LocalDate,
    modifier: Modifier = Modifier,
    questionExpanded: Boolean = true,
    onSubmitClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onQuestionToggle: () -> Unit = {},
    onAnswerChanged: (String) -> Unit = {},
    onSaveDraftClick: () -> Unit = {},
    onDraftListClick: () -> Unit = {},
    onMediaPicked: (suspend (uriString: String) -> String?)? = null,
) {
    Scaffold(
        topBar = {
            // Figma 2372:22546 — 상단바: 뒤로가기 / 가운데 날짜 / 우측 저장 버튼
            DetailTopBar(
                title = date.format(TopBarDateFormatter),
                actions = {
                    Button(
                        onClick = onSubmitClick,
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
                            text = stringResource(R.string.mindrecord_action_save),
                            style = AfternoteDesign.typography.bodySmallB,
                        )
                    }
                },
                onBackClick = onBackClick,
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
            // Figma 2372:22574 — 오늘의 추천 질문 배너 (접기/펼치기)
            val questionLoadErrorText = uiState.questionLoadError?.asString()
            val questionText =
                when {
                    uiState.isQuestionLoading -> stringResource(R.string.mindrecord_daily_question_write_loading)
                    questionLoadErrorText != null -> questionLoadErrorText
                    uiState.questionContent.isNotEmpty() -> uiState.questionContent
                    else -> stringResource(R.string.mindrecord_daily_question_write_none)
                }
            DailyQuestionBanner(
                questionText = questionText,
                expanded = questionExpanded,
                onToggle = onQuestionToggle,
                dayNumber = uiState.questionDay,
            )

            val errorMessage = (uiState.submitState as? SubmitState.Failed)?.message?.asString()
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
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
                        stringResource(R.string.mindrecord_write_saving)
                    }

                    uiState.isUploadingImage -> {
                        stringResource(R.string.mindrecord_write_uploading_image)
                    }

                    else -> {
                        null
                    }
                }
            if (progressText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progressText,
                    color = AfternoteDesign.colors.gray6,
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            val uploadError = uiState.imageUploadError?.asString()
            if (uploadError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uploadError,
                    color = AfternoteDesign.colors.error,
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            // 프리필이 늦게 도착해도 WriteTextField 가 value 변경에 반응해 다시 시드한다 —
            // key() 로 컴포넌트를 재생성하지 않는다 (#1018).
            WriteTextField(
                value = uiState.answer,
                onValueChange = onAnswerChanged,
                onSaveDraftClick = onSaveDraftClick,
                onDraftCountClick = onDraftListClick,
                draftCount = uiState.draftCount,
                onImagePicked = onMediaPicked,
                onMediaPicked = onMediaPicked,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteScreenPreview() {
    AfternoteTheme {
        DailyQuestionWriteScreenContent(
            uiState =
                DailyQuestionWriteUiState(
                    questionId = 28L,
                    questionDay = 28,
                    questionContent = "오늘 가장 오래 기억하고 싶은 순간은 무엇인가요?",
                    answer = "<p>가족과 늦은 저녁을 먹으며 웃었던 순간.</p>",
                    isQuestionLoading = false,
                    draftCount = 1,
                ),
            date = LocalDate.of(2026, 8, 28),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
