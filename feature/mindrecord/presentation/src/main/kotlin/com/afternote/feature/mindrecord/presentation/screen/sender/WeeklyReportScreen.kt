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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.EmotionKeywordCard
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

    when (val state = uiState) {
        WeeklyReportUiState.Loading -> {
            LoadingBox(modifier)
        }

        is WeeklyReportUiState.Error -> {
            ErrorBox(message = state.message.asString(), modifier = modifier)
        }

        is WeeklyReportUiState.Success -> {
            WeeklyReportContent(
                state = state,
                onWeekSelect = viewModel::selectWeek,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun WeeklyReportContent(
    state: WeeklyReportUiState.Success,
    onWeekSelect: (java.time.LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma 노드 852:11543 — main 컨테이너의 섹션 간 gap=32, 시작 pt=8, 끝 pb=200.
    // 가로 패딩(20dp)은 호출부(HomeScreen)에서 이미 제공하므로 여기선 추가하지 않는다.
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
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
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray9,
                    modifier = Modifier.padding(start = 8.dp),
                )
                WeeklyMoodCalendar(days = state.weekDays)
            }
        }

        // TOP KEYWORDS 섹션 — py=8, gap=12 (divider + 감정 카드).
        // INSIGHTS 는 감정 키워드 카드 안으로 통합됨 (Figma 2249:13964).
        item {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeader(title = "TOP KEYWORDS")
                EmotionKeywordCard(
                    keywords = state.emotionKeywords,
                    insightText =
                        if (state.emotionKeywords.isEmpty()) {
                            stringResource(R.string.mindrecord_emotion_card_empty_description, state.userName)
                        } else {
                            state.summaryText
                        },
                )
            }
        }

        // HISTORY 섹션 — gap=12 (divider + 다이어리 카드들).
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = "HISTORY")
                state.dailyQuestions.forEach { dailyQuestion ->
                    DailyQuestionListCard(answer = dailyQuestion)
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
    val prefix = stringResource(R.string.mindrecord_weekly_report_recorded_prefix)
    val middle = stringResource(R.string.mindrecord_weekly_report_recorded_middle)
    val daysText = stringResource(R.string.mindrecord_weekly_report_days_format, recordedDays)
    val suffix = stringResource(R.string.mindrecord_weekly_report_recorded_suffix)
    // Figma 852:11578 — 16sp Regular, 이름·일수만 b1 컬러 강조.
    return buildAnnotatedString {
        append(prefix)
        withStyle(style = SpanStyle(color = AfternoteDesign.colors.b1)) {
            append(userName)
        }
        append(middle)
        withStyle(style = SpanStyle(color = AfternoteDesign.colors.b1)) {
            append(daysText)
        }
        append(suffix)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray6,
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 12.dp),
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
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
                    summaryText = "이번 주는 차분히 마음을 정리한 한 주였어요.",
                    dailyQuestions = emptyList(),
                ),
            onWeekSelect = {},
        )
    }
}
