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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import java.time.YearMonth

@Composable
fun DailyQuestionAnswerListScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    viewModel: DailyQuestionListViewModel = hiltViewModel(),
) {
    // 갱신을 이 화면이 직접 건다. HomeScreen 이 VM 을 호이스팅해 대신 걸어 주면, 탭에
    // 들어가지 않아도 VM 이 만들어져 `init` 조회가 미리 나간다 (#736).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

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
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val onYearMonthChanged: (YearMonth) -> Unit = { yearMonth = it }
    // 월이 바뀌면 그 달의 선택은 무효다.
    var selectedDay by remember(yearMonth) { mutableStateOf<Int?>(null) }

    // 캘린더 형에서는 **선택한 월** 기준으로 카드도 함께 좁힌다. 종전에는 캘린더 점만
    // 필터하고 카드는 전체 기간을 그대로 렌더해, 8월 캘린더가 "0개의 답변 완료" 인데
    // 아래에 7월 답변이 남아 있었다 (#724).
    val monthAnswers =
        if (isListView) {
            answers
        } else {
            answers.filter { it.date.year == yearMonth.year && it.date.monthValue == yearMonth.monthValue }
        }
    val visibleAnswers =
        selectedDay?.let { day -> monthAnswers.filter { it.date.dayOfMonth == day } } ?: monthAnswers

    // 리스트 형에서 전체가 비었을 때만 캘린더 없이 빈 상태를 보여준다. 캘린더 형에서는
    // 빈 상태가 캘린더를 대체하면 월 이동 수단이 사라진다 (#724).
    if (isListView && answers.isEmpty()) {
        MindRecordEmptyState(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Figma 2671:16704 — 캘린더 형은 캘린더 아래에 카드 리스트가 바로 이어짐 (gap 24)
        if (!isListView) {
            val answeredDays = monthAnswers.map { it.date.dayOfMonth }.toSet()
            item {
                DailyCalendar(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    type = MindRecordCategoryUi.DailyQuestion,
                    onPrevMonth = { onYearMonthChanged(yearMonth.minusMonths(1)) },
                    onNextMonth = { onYearMonthChanged(yearMonth.plusMonths(1)) },
                    answeredDays = answeredDays,
                    selectedDay = selectedDay,
                    // 같은 날을 다시 누르면 선택을 푼다 — 그 달 전체로 돌아올 수단이 필요하다.
                    onDayClick = { day -> selectedDay = if (selectedDay == day) null else day },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        // Figma 2757:16116 — 리스트 형은 답변 카드만 나열
        if (visibleAnswers.isEmpty()) {
            item { MindRecordEmptyState() }
        }

        items(visibleAnswers, key = { it.id }) { answer ->
            DailyQuestionListCard(answer = answer, onDelete = { onDelete(answer.id) })
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
            answers = emptyList(),
        )
    }
}
