package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionBanner
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.DeleteConfirmDialog
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.component.MindRecordErrorBox
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.TodayQuestionUi
import java.time.YearMonth

@Composable
fun DailyQuestionAnswerListScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    onEditClick: (Long) -> Unit = {},
    viewModel: DailyQuestionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    pendingDeleteId?.let { id ->
        DeleteConfirmDialog(
            onConfirm = {
                pendingDeleteId = null
                viewModel.delete(id)
            },
            onDismiss = { pendingDeleteId = null },
        )
    }

    when (val state = uiState) {
        DailyQuestionListUiState.Loading -> {
            LoadingBox(modifier)
        }

        is DailyQuestionListUiState.Error -> {
            MindRecordErrorBox(
                message = state.message.asString(),
                onRetry = viewModel::retry,
                modifier = modifier,
            )
        }

        is DailyQuestionListUiState.Success -> {
            // 배너와 리스트를 형제 루트 2개로 내보내면 안 된다 — 이 화면들의 유일한 호출부가
            // HorizontalPager 페이지라, 다중 placeable 이 가로로 순차 배치돼 배너가 뜨는 순간
            // 리스트가 배너 폭만큼 밀려 페이지 밖으로 잘린다 (리뷰 지적).
            Column(modifier = modifier) {
                // 삭제 실패 안내 — 항목이 남은 채 아무 말이 없으면 고장처럼 보인다 (#716).
                val deleteError = state.deleteError?.asString()
                if (deleteError != null) {
                    Text(
                        text = deleteError,
                        color = AfternoteDesign.colors.error,
                        style = AfternoteDesign.typography.captionLargeR,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                DailyQuestionListContent(
                    isListView = isListView,
                    todayQuestion = state.todayQuestion,
                    answers = state.answers,
                    onEdit = onEditClick,
                    // 삭제는 되돌릴 수 없다 — 종전에는 메뉴를 누르는 즉시 실행됐다 (#582).
                    onDelete = { pendingDeleteId = it },
                )
            }
        }
    }
}

@Composable
private fun DailyQuestionListContent(
    isListView: Boolean,
    answers: List<DailyQuestion>,
    modifier: Modifier = Modifier,
    todayQuestion: TodayQuestionUi? = null,
    onEdit: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
) {
    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
    val onYearMonthChanged: (YearMonth) -> Unit = { yearMonth = it }
    var questionExpanded by remember { mutableStateOf(true) }

    // 답변이 0건이어도 오늘의 추천 질문은 보여야 한다 — 종전에는 빈 상태가 화면 전체를
    // 대체해 이 영역까지 함께 사라졌다 (#592).
    if (isListView && answers.isEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TodayRecommendedQuestion(
                todayQuestion = todayQuestion,
                expanded = questionExpanded,
                onToggle = { questionExpanded = !questionExpanded },
            )
            MindRecordEmptyState()
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Figma 2671:15631 — 답변 목록 위의 "오늘의 추천 질문" 영역.
        item {
            TodayRecommendedQuestion(
                todayQuestion = todayQuestion,
                expanded = questionExpanded,
                onToggle = { questionExpanded = !questionExpanded },
            )
        }

        // Figma 2671:16704 — 캘린더 형은 캘린더 아래에 카드 리스트가 바로 이어짐 (gap 24)
        if (!isListView) {
            val answeredDays =
                answers
                    .filter { it.date.year == yearMonth.year && it.date.monthValue == yearMonth.monthValue }
                    .map { it.date.dayOfMonth }
                    .toSet()
            item {
                DailyCalendar(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    type = MindRecordCategoryUi.DailyQuestion,
                    onPrevMonth = { onYearMonthChanged(yearMonth.minusMonths(1)) },
                    onNextMonth = { onYearMonthChanged(yearMonth.plusMonths(1)) },
                    answeredDays = answeredDays,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        // Figma 2757:16116 — 리스트 형은 답변 카드만 나열
        items(answers, key = { it.id }) { answer ->
            DailyQuestionListCard(
                answer = answer,
                onEdit = { onEdit(answer.id) },
                onDelete = { onDelete(answer.id) },
            )
        }
    }
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
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

/**
 * 목록 상단의 "오늘의 추천 질문".
 *
 * 조회가 실패하면 `todayQuestion` 이 `null` 이라 영역을 그리지 않는다 — 목록 자체는
 * 정상이므로 보조 영역 하나 때문에 화면을 오류로 바꾸지 않는다. 실패를 어떻게 알릴지는
 * 시안이 정의하지 않아 표현을 새로 만들지 않았다 (#592).
 */
@Composable
private fun TodayRecommendedQuestion(
    todayQuestion: TodayQuestionUi?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    if (todayQuestion == null) return
    DailyQuestionBanner(
        questionText = todayQuestion.content,
        expanded = expanded,
        onToggle = onToggle,
        dayNumber = todayQuestion.day,
    )
}
