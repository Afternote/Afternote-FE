package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.asString
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.EmotionKeywordCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordErrorBox
import com.afternote.feature.mindrecord.presentation.component.WeeklyMoodCalendar
import com.afternote.feature.mindrecord.presentation.component.WeeklyReportReviewCard
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel

@Composable
fun WeeklyReportScreen(
    modifier: Modifier = Modifier,
    viewModel: WeeklyReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 갱신을 이 화면이 직접 건다. HomeScreen 이 VM 을 호이스팅해 대신 걸어 주면, 탭에
    // 들어가지 않아도 VM 이 만들어져 `init` 조회가 미리 나간다 (#736).
    //
    // 첫 ON_RESUME 을 건너뛰는 판단은 ViewModel 이 한다 — 화면 저장 상태에 두면 프로세스
    // 사망 뒤 «지나갔음» 만 복원돼 새 ViewModel 의 init 과 겹친다 (#736 리뷰).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    when (val state = uiState) {
        WeeklyReportUiState.Loading -> {
            LoadingBody(modifier)
        }

        is WeeklyReportUiState.Error -> {
            // 보여 줄 리포트가 없어도 주차 선택과 재시도는 남긴다 — 둘 다 없으면 이 화면에서
            // 빠져나갈 방법이 없다 (#723).
            WeeklyReportErrorContent(
                state = state,
                onWeekSelect = viewModel::selectWeek,
                onRetry = viewModel::retry,
                modifier = modifier,
            )
        }

        is WeeklyReportUiState.Success -> {
            WeeklyReportContent(
                state = state,
                onWeekSelect = viewModel::selectWeek,
                onEmotionAnalysisRetry = viewModel::retryEmotionAnalysis,
                onRetry = viewModel::retry,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun WeeklyReportContent(
    state: WeeklyReportUiState.Success,
    onWeekSelect: (java.time.LocalDate) -> Unit,
    onEmotionAnalysisRetry: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma 노드 852:11543 — main 컨테이너의 섹션 간 gap=32, 시작 pt=8, 끝 pb=200.
    // 가로 패딩(20dp)은 호출부(HomeScreen)에서 이미 제공하므로 여기선 추가하지 않는다.
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        state.loadFailure?.let { failure ->
            item {
                WeekLoadFailureBanner(
                    failure = failure,
                    onRetry = onRetry,
                )
            }
        }

        item {
            WeeklyReportReviewCard(
                selectedMonday = state.selectedMonday,
                weekOptions = state.weekOptions,
                onWeekSelect = onWeekSelect,
                dateRange = state.dateRange,
                counts = state.counts,
            )
        }

        // 요약 메시지 + 캘린더 — gap=8, 메시지 좌측 pl=8.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = recordedSummary(userName = state.userName, recordedDays = state.recordedDays),
                    modifier = Modifier.padding(start = 8.dp),
                )
                WeeklyMoodCalendar(days = state.weekDays)
            }
        }

        // TOP KEYWORDS 섹션 — py=8, gap=12 (divider + 감정 카드). Figma 852:11581 에는 INSIGHT 카드 없음.
        item {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AfternoteSectionHeader(title = "TOP KEYWORDS")
                EmotionKeywordCard(
                    keywords = state.emotionKeywords,
                    descriptionText = emotionCardDescription(state),
                    analysisStatus = state.emotionAnalysisStatus,
                    onRetry = onEmotionAnalysisRetry,
                )
            }
        }

        // HISTORY 섹션 — gap=12 (divider + 다이어리 카드들).
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AfternoteSectionHeader(title = "HISTORY")
                state.dailyQuestions.forEach { dailyQuestion ->
                    // 주간 리포트는 읽기 전용 요약이다 — 편집·삭제는 데일리질문 목록의 몫이라
                    // 여기서는 «더보기» 를 그리지 않는다 (#1540).
                    // 주간 리포트는 **읽기 전용 요약**이다 — 편집·삭제도, 카드 탭도 없다.
                    // 셋 다 `null` 이라 클릭 semantics 자체가 안 붙는다: no-op 을 넘기면
                    // 스크린리더가 「버튼」으로 읽는데 눌러도 아무 일이 없다 (#1540 리뷰).
                    DailyQuestionListCard(
                        answer = dailyQuestion,
                        onClick = null,
                        onEdit = null,
                        onDelete = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun recordedSummary(
    userName: String,
    recordedDays: Int,
): androidx.compose.ui.text.AnnotatedString {
    val daysText = stringResource(R.string.mindrecord_weekly_report_days_format, recordedDays)
    val sentence = stringResource(R.string.mindrecord_weekly_report_recorded, userName, daysText)
    val highlights = recordedSummaryHighlights(sentence, userName, daysText)
    val base = AfternoteDesign.typography.bodyLargeB
    return buildAnnotatedString {
        append(sentence)
        addStyle(base.toSpanStyle(), 0, sentence.length)
        highlights.forEach { range ->
            addStyle(
                base.copy(color = AfternoteDesign.colors.b1).toSpanStyle(),
                range.first,
                range.last + 1,
            )
        }
    }
}

/**
 * 완성된 문장에서 강조할 구간(이름·기록일수)을 찾는다.
 *
 * 문장을 조각으로 나눠 이어 붙이지 않고 한 리소스로 두기 위한 것이다 — 조각 방식은 리소스
 * 앞뒤 공백에 의존하는데, aapt2 가 그 공백을 지워 APK 에서 문구가 붙어 버렸다 (#732).
 *
 * 이름을 먼저 찾고 기록일수는 그 뒤에서 찾는다. 이름에 "3일" 같은 문자열이 들어 있어도
 * 강조 구간이 겹치지 않는다. 찾지 못하면 강조를 생략한다 — 문장은 그대로 보인다.
 */
internal fun recordedSummaryHighlights(
    sentence: String,
    userName: String,
    daysText: String,
): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    val nameStart = sentence.indexOf(userName).takeIf { it >= 0 && userName.isNotEmpty() }
    val searchFrom =
        if (nameStart != null) {
            ranges += nameStart until (nameStart + userName.length)
            nameStart + userName.length
        } else {
            0
        }
    val daysStart = sentence.indexOf(daysText, startIndex = searchFrom).takeIf { it >= 0 && daysText.isNotEmpty() }
    if (daysStart != null) ranges += daysStart until (daysStart + daysText.length)
    return ranges
}

@Composable
private fun ErrorBox(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = AfternoteDesign.colors.gray9)
    }
}

/**
 * 조회에 실패했지만 직전 리포트가 남아 있을 때의 배너 (#723).
 *
 * 어느 주차를 못 불렀는지 밝히고 재시도 수단을 준다. 화면 아래에는 직전에 성공한
 * 리포트가 그대로 남아 있어 다른 주차로 옮길 수도 있다.
 */
@Composable
private fun WeekLoadFailureBanner(
    failure: WeeklyReportUiState.LoadFailure,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text =
                stringResource(
                    R.string.mindrecord_weekly_report_load_failed,
                    failure.failedWeekLabel.monthValue,
                    (failure.failedWeekLabel.dayOfMonth - 1) / 7 + 1,
                ),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray9,
        )
        Text(
            text = failure.message.asString(),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.mindrecord_weekly_report_retry),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

/**
 * 보여 줄 리포트가 하나도 없는 실패 화면 (#723).
 *
 * 오류 문구만 렌더하면 주차 선택 UI 까지 사라져 이 상태에서 빠져나갈 수 없다.
 * 주차 드롭다운과 재시도를 함께 남긴다.
 */
@Composable
private fun WeeklyReportErrorContent(
    state: WeeklyReportUiState.Error,
    onWeekSelect: (java.time.LocalDate) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WeeklyReportReviewCard(
            selectedMonday = state.failedMonday,
            weekOptions = state.weekOptions,
            onWeekSelect = onWeekSelect,
            dateRange = "",
            counts = emptyList(),
        )
        Text(
            text = state.message.asString(),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        TextButton(onClick = onRetry) {
            Text(
                text = stringResource(R.string.mindrecord_weekly_report_retry),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

/**
 * 감정 카드 본문 문구.
 *
 * 키워드가 없을 때 무엇을 적을지는 **분석 상태**가 정한다. 종전에는 빈 목록이면 무조건
 * "키워드가 나오지 않았어요" 를 적어, 분석 대기·실패까지 정상 빈 상태로 확정했다 (#725).
 *
 * **상태를 키워드보다 먼저 본다.** 부분 성공(일부 완료 + 일부 대기)에서는 완료분 키워드가
 * `emotions` 에 실려 내려오므로(BE `buildTopEmotions` 에 완료 게이트가 없다), 키워드 유무를
 * 먼저 보면 아직 분석 중인데도 폴백 요약이 최종 요약처럼 확정된다.
 *
 * 그래서 대기 중에는 키워드가 이미 몇 개 있어도 «분석 중» 임을 알린다 — 지금 보이는 것이
 * 전부가 아니라는 사실이 요약 문구보다 중요하다.
 */
@Composable
internal fun emotionCardDescription(state: WeeklyReportUiState.Success): String =
    when (state.emotionAnalysisStatus) {
        EmotionAnalysisStatus.PENDING -> {
            if (state.emotionKeywords.isEmpty()) {
                stringResource(R.string.mindrecord_emotion_card_pending_description)
            } else {
                // 일부는 이미 나왔다 — 이 요약이 최종이 아니라는 것만 덧붙인다.
                stringResource(R.string.mindrecord_weekly_report_summary_pending)
            }
        }

        EmotionAnalysisStatus.FAILED -> {
            stringResource(R.string.mindrecord_emotion_card_failed_description)
        }

        EmotionAnalysisStatus.UNKNOWN -> {
            // 0 건인지 분석 중인지 모른다 — 어느 쪽으로도 확정하지 않는다.
            if (state.emotionKeywords.isEmpty()) {
                stringResource(R.string.mindrecord_emotion_card_unknown_description)
            } else {
                state.summaryText
            }
        }

        EmotionAnalysisStatus.NOTHING_TO_ANALYZE, EmotionAnalysisStatus.COMPLETED -> {
            if (state.emotionKeywords.isNotEmpty()) {
                state.summaryText
            } else {
                stringResource(R.string.mindrecord_emotion_card_empty_description, state.userName)
            }
        }
    }

@Preview(showBackground = true)
@Composable
private fun WeeklyReportScreenPreview() {
    AfternoteTheme {
        WeeklyReportContent(
            state =
                WeeklyReportUiState.Success(
                    selectedMonday = java.time.LocalDate.of(2025, 11, 10),
                    weekOptions = emptyList(),
                    dateRange = "2025.11.10. - 2025.11.16.",
                    userName = "박서연",
                    recordedDays = 3,
                    counts = emptyList(),
                    weekDays = emptyList(),
                    emotionKeywords = emptyList(),
                    emotionAnalysisStatus = EmotionAnalysisStatus.COMPLETED,
                    summaryText = "이번 주는 차분히 마음을 정리한 한 주였어요.",
                    dailyQuestions = emptyList(),
                ),
            onWeekSelect = {},
            onEmotionAnalysisRetry = {},
            onRetry = {},
        )
    }
}
