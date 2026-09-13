package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
    onSubmitSuccess: () -> Unit,
    onBackClick: () -> Unit,
    onDraftListClick: () -> Unit,
    viewModel: DailyQuestionWriteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSubmitSuccess by rememberUpdatedState(onSubmitSuccess)

    LaunchedEffect(uiState.submitState) {
        if (uiState.submitState is SubmitState.Succeeded) {
            currentOnSubmitSuccess()
            viewModel.consumeSubmitResult()
        }
    }

    DailyQuestionWriteScreenContent(
        uiState = uiState,
        // 현재 시각을 화면 밖에서 받는다 — 안에서 LocalDate.now() 를 부르면 screenshotTest
        // baseline 이 날마다 달라진다 (#1359).
        date = LocalDate.now(),
        modifier = modifier,
        onBackClick = onBackClick,
        onSubmit = { viewModel.submit() },
        onSaveDraft = { viewModel.submit(isDraft = true) },
        onDraftListClick = onDraftListClick,
        onAnswerChanged = viewModel::onAnswerChanged,
        onMediaPicked = viewModel::uploadMedia,
        onRetryResumeDraft = viewModel::retryResumeDraft,
    )
}

/**
 * ViewModel 과 현재 시각에서 분리된 데일리질문 작성 화면 본문 (#1359).
 *
 * screenshotTest 가 고정 상태를 그대로 렌더할 수 있도록 상태는 [uiState], 날짜는 [date] 로
 * 받고 이벤트는 콜백으로 받는다. 제출 성공 신호는 화면 밖 이동이라 래퍼가 소유한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DailyQuestionWriteScreenContent(
    uiState: DailyQuestionWriteUiState,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit,
    onSaveDraft: () -> Unit,
    onDraftListClick: () -> Unit,
    onAnswerChanged: (String) -> Unit,
    onMediaPicked: suspend (String) -> String? = { null },
    onRetryResumeDraft: () -> Unit,
) {
    // 배너 접힘은 이 화면 안에서만 의미가 있는 표시 상태다 — Content 가 소유한다.
    var questionExpanded by remember { mutableStateOf(true) }

    // 키보드가 뜨면 질문 배너를 접는다 (#1860).
    //
    // 이 화면의 세로 배분은 «질문 배너 + 툴바가 먼저, 본문은 남는 것» 이다. edge-to-edge 라
    // 키보드가 떠도 윈도우는 줄지 않고 IME inset 만 오므로(#1849), 그 inset 만큼이 화면 안에서
    // 빠진다. 720x1280 @320dpi · font_scale 1.30 에서는 남는 높이가 0 이 돼 본문 칸이 통째로
    // 눌렸다 — 타이핑은 되는데 쓴 글이 안 보였다.
    //
    // 접는 것은 «자동 1회» 다. 사용자가 다시 펼칠 수 있고, 그 선택을 키보드가 다시 덮지 않는다.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible) questionExpanded = false
    }

    Scaffold(
        topBar = {
            // Figma 2372:22546 — 상단바: 뒤로가기 / 가운데 날짜 / 우측 저장 버튼
            DetailTopBar(
                title = date.format(TopBarDateFormatter),
                actions = {
                    Button(
                        onClick = onSubmit,
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
        BoxWithConstraints(
            modifier =
                Modifier
                    .padding(paddingValues)
                    // IME inset 을 본문 영역 전체에서 한 번에 뺀다. 종전에는 하단 툴바만
                    // imePadding() 을 들어, 키보드 높이가 «툴바 아래 여백» 으로 화면 안에서
                    // 빠지면서도 레이아웃 계산에서는 본문과 같은 자리를 두고 다퉜다.
                    // consumeWindowInsets 가 없으면 Scaffold 가 이미 넘긴 내비 바 몫을
                    // imePadding() 이 한 번 더 더한다 (#1860 · #1891).
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
                    .fillMaxSize(),
        ) {
            // 본문 칸은 «남는 것» 을 받는 마지막 자리라, 위쪽이 다 먹으면 0 이 된다. 위쪽이
            // 가져갈 수 있는 높이에 상한을 걸어 본문 자리를 먼저 떼어 둔다 — 상한을 넘는
            // 안내 문구는 그 안에서 스크롤된다 (#1860).
            val headerMaxHeight = (maxHeight - MinimumWriteAreaHeight).coerceAtLeast(0.dp)
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
            ) {
                Column(
                    modifier =
                        Modifier
                            .heightIn(max = headerMaxHeight)
                            .verticalScroll(rememberScrollState()),
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
                        onToggle = { questionExpanded = !questionExpanded },
                        dayNumber = uiState.questionDay,
                    )

                    // 이어쓰기 조회 실패는 저장 실패와 다른 시점·다른 원인이라 따로 알린다. 삼키면
                    // 사용자가 빈 화면을 «임시저장 없음» 으로 읽고 저장해 기존 임시저장을 덮는다 (#1018).
                    val draftResumeErrorText = uiState.draftResumeError?.asString()
                    if (draftResumeErrorText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = draftResumeErrorText,
                            color = AfternoteDesign.colors.error,
                            style = AfternoteDesign.typography.captionLargeR,
                        )
                        // 실패 동안 저장이 막히므로 화면 안에서 풀 수단을 준다 — 없으면 쓴 답변을 들고
                        // 갇힌다. 이 모듈의 재시도는 TextButton 으로 그린다 (#1019).
                        TextButton(onClick = onRetryResumeDraft) {
                            Text(
                                text = stringResource(R.string.mindrecord_error_retry),
                                style = AfternoteDesign.typography.bodySmallB,
                                color = AfternoteDesign.colors.gray9,
                            )
                        }
                    }

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
                }

                Spacer(modifier = Modifier.height(16.dp))
                // 프리필이 늦게 도착해도 WriteTextField 가 value 변경에 반응해 다시 시드한다 —
                // key() 로 컴포넌트를 재생성하지 않는다 (#1018).
                WriteTextField(
                    value = uiState.answer,
                    onValueChange = onAnswerChanged,
                    onSaveDraftClick = onSaveDraft,
                    onDraftCountClick = onDraftListClick,
                    draftCount = uiState.draftCount,
                    onImagePicked = onMediaPicked,
                    onMediaPicked = onMediaPicked,
                )
            }
        }
    }
}

/**
 * 본문 입력 칸과 하단 툴바가 함께 서기 위한 최소 높이 (#1860).
 *
 * 하단 툴바는 아이콘 버튼 행이라 글꼴 배율을 따라 자란다 — 배율 1.30 실측 120dp. 거기에
 * 에디터 안쪽 여백(위아래 16dp)과 본문 한 줄을 더했다. 좁은 화면에서 이만큼도 안 남으면
 * 질문 배너와 안내 문구 쪽이 줄고 넘치는 부분은 그 영역 안에서 스크롤된다 — 본문 칸이
 * 먼저 사라지던 종전 배분을 뒤집는다.
 *
 * 툴바가 더 자라면 이 값도 모자라진다. 그 순간을 눈이 아니라 테스트가 잡도록
 * `DailyQuestionWriteKeyboardSpaceTest` 가 실측 조합으로 본문 높이를 단언한다.
 */
private val MinimumWriteAreaHeight = 200.dp

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteScreenPreview() {
    AfternoteTheme {
        // Preview는 ViewModel 없이 호출 불가 — 컴파일 확인용 placeholder
    }
}
