package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionWriteHeaderCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.TodayQuestionUi
import java.time.LocalDate

@Composable
fun DailyQuestionAnswerListScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    viewModel: DailyQuestionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        DailyQuestionListUiState.Loading -> {
            LoadingBox(modifier)
        }

        is DailyQuestionListUiState.Error -> {
            ErrorBox(message = state.message.asString(), modifier = modifier)
        }

        is DailyQuestionListUiState.Success -> {
            DailyQuestionListContent(
                modifier = modifier,
                isListView = isListView,
                todayQuestion = state.todayQuestion,
                answers = state.answers,
            )
        }
    }
}

@Composable
private fun DailyQuestionListContent(
    isListView: Boolean,
    todayQuestion: TodayQuestionUi?,
    answers: List<DailyQuestion>,
    modifier: Modifier = Modifier,
) {
    if (isListView && todayQuestion == null && answers.isEmpty()) {
        MindRecordEmptyState(modifier = modifier)
        return
    }

    val headerText = todayQuestion?.content ?: stringResource(R.string.mindrecord_daily_question_write_none)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isListView) {
            item { DailyQuestionWriteHeaderCard(questionText = headerText) }
            items(answers, key = { it.id }) { DailyQuestionListCard(answer = it) }
        } else {
            val today = LocalDate.now()
            val answeredDays =
                answers
                    .filter { it.date.year == today.year && it.date.monthValue == today.monthValue }
                    .map { it.date.dayOfMonth }
                    .toSet()
            item {
                DailyCalendar(
                    year = today.year,
                    month = today.monthValue,
                    type = MindRecordCategoryUi.DailyQuestion,
                    onPrevMonth = {},
                    onNextMonth = {},
                    answeredDays = answeredDays,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DAILY ANSWER",
                        style = AfternoteDesign.typography.mono,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            item { DailyQuestionWriteHeaderCard(questionText = headerText) }
            items(answers, key = { it.id }) { DailyQuestionListCard(answer = it) }
        }
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
private fun DailyQuestionAnswerListScreenPreviewFalse() {
    AfternoteTheme {
        DailyQuestionListContent(
            modifier = Modifier,
            isListView = false,
            todayQuestion = null,
            answers = emptyList(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionAnswerListScreenPreviewTrue() {
    AfternoteTheme {
        DailyQuestionListContent(
            modifier = Modifier,
            isListView = true,
            todayQuestion = null,
            answers = emptyList(),
        )
    }
}
