package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.EmotionKeywordCard
import com.afternote.feature.mindrecord.presentation.component.InsightCard
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
            ErrorBox(message = state.message, modifier = modifier)
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
    LazyColumn(modifier = modifier) {
        item {
            WeeklyReportReviewCard(
                selectedMonday = state.selectedMonday,
                weekOptions = state.weekOptions,
                onWeekSelect = onWeekSelect,
                dateRange = state.dateRange,
                counts = state.counts,
            )
        }

        item {
            Text(text = recordedSummary(userName = state.userName, recordedDays = state.recordedDays))
        }

        item {
            WeeklyMoodCalendar(days = state.weekDays)
        }

        item {
            SectionHeader(title = "TOP KEYWORDS")
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            EmotionKeywordCard(
                bubbles = state.emotionBubbles,
                descriptionText = state.summaryText,
            )
        }

        item {
            InsightCard(bodyText = state.summaryText)
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            SectionHeader(title = "HISTORY")
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(state.dailyQuestions, key = { it.id to it.date }) { dailyQuestion ->
            DailyQuestionListCard(answer = dailyQuestion)
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
    return buildAnnotatedString {
        withStyle(style = AfternoteDesign.typography.bodyLargeB.toSpanStyle()) {
            append(prefix)
            withStyle(
                style =
                    AfternoteDesign.typography.bodyLargeB
                        .copy(color = AfternoteDesign.colors.b1)
                        .toSpanStyle(),
            ) {
                append(userName)
            }
            append(middle)
            withStyle(
                style =
                    AfternoteDesign.typography.bodyLargeB
                        .copy(color = AfternoteDesign.colors.b1)
                        .toSpanStyle(),
            ) {
                append(daysText)
            }
            append(suffix)
        }
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
            color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
        )
        HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
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
                    emotionBubbles = emptyList(),
                    summaryText = "이번 주는 차분히 마음을 정리한 한 주였어요.",
                    dailyQuestions = emptyList(),
                ),
            onWeekSelect = {},
        )
    }
}
