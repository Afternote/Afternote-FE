package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel
import java.time.LocalDate
import java.time.YearMonth

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
                answers = state.answers,
                onDelete = viewModel::delete,
            )
        }
    }
}

@Composable
private fun DailyQuestionListContent(
    isListView: Boolean,
    answers: List<DailyQuestion>,
    modifier: Modifier = Modifier,
    onDelete: (Long) -> Unit = {},
) {
    if (isListView) {
        // Figma 2757:16116 — 리스트 형: 답변 카드만 8dp 간격으로 나열
        if (answers.isEmpty()) {
            MindRecordEmptyState(modifier = modifier)
            return
        }

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(answers, key = { it.id }) { answer ->
                DailyQuestionListCard(
                    answer = answer,
                    recipientNames = answer.receiverNames.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    imageUrl = answer.imageUrl,
                    onDelete = { onDelete(answer.id) },
                )
            }
        }
    } else {
        // Figma 2671:16704 / 2671:16718 — 캘린더 형: 캘린더 + 선택한 날짜의 답변 카드
        var yearMonth by remember { mutableStateOf(YearMonth.now()) }
        var selectedDay by remember { mutableStateOf<Int?>(null) }

        val monthAnswers =
            answers.filter { it.date.year == yearMonth.year && it.date.monthValue == yearMonth.monthValue }
        val answeredDays = monthAnswers.map { it.date.dayOfMonth }.toSet()
        val visibleAnswers =
            selectedDay?.let { day -> monthAnswers.filter { it.date.dayOfMonth == day } } ?: monthAnswers

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                DailyCalendar(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    type = MindRecordCategoryUi.DailyQuestion,
                    onPrevMonth = {
                        yearMonth = yearMonth.minusMonths(1)
                        selectedDay = null
                    },
                    onNextMonth = {
                        yearMonth = yearMonth.plusMonths(1)
                        selectedDay = null
                    },
                    answeredDays = answeredDays,
                    selectedDay = selectedDay,
                    onDayClick = { day ->
                        selectedDay = if (selectedDay == day) null else day
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            items(visibleAnswers, key = { it.id }) { answer ->
                DailyQuestionListCard(
                    answer = answer,
                    recipientNames = answer.receiverNames.takeIf { it.isNotEmpty() }?.joinToString(", "),
                    imageUrl = answer.imageUrl,
                    onDelete = { onDelete(answer.id) },
                )
            }
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
            answers =
                listOf(
                    DailyQuestion(
                        id = 1L,
                        title = "오늘 하루, 누구에게 가장 고마웠나요?",
                        content = "아무 말 없이 그저 나의 곁을 지켜주는 아내가 고맙다.",
                        date = LocalDate.now(),
                    ),
                ),
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
            answers =
                listOf(
                    DailyQuestion(
                        id = 1L,
                        title = "오늘 하루, 누구에게 가장 고마웠나요?",
                        content = "아무 말 없이 그저 나의 곁을 지켜주는 아내가 고맙다.",
                        date = LocalDate.now(),
                    ),
                ),
        )
    }
}
