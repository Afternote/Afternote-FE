package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.afternote.core.ui.button.FAB.AfternoteFabContentBottomPadding
import com.afternote.core.ui.loading.LoadingBody
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

/**
 * @param onItemClick 카드를 눌렀을 때 상세로 보낼 기록 ID 와 **보고 있는 달** (#759 리뷰).
 *   기본값을 두지 않는다 —
 *   기본값이 있으면 호출부에서 빠뜨려도 컴파일이 되고, 카드가 눌리지 않는 채로 나간다 (#759).
 */
@Composable
fun DailyQuestionAnswerListScreen(
    onItemClick: (Long, YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    onEditClick: (Long) -> Unit,
    viewModel: DailyQuestionListViewModel = hiltViewModel(),
) {
    // 갱신을 이 화면이 직접 건다. HomeScreen 이 VM 을 호이스팅해 대신 걸어 주면, 탭에
    // 들어가지 않아도 VM 이 만들어져 `init` 조회가 미리 나간다 (#736).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

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
            LoadingBody(modifier)
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
                    onItemClick = onItemClick,
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
    /** 항목 탭 — 저장된 기록 본문을 여는 상세 화면 (#759). */
    onItemClick: (Long, YearMonth) -> Unit,
    /** «수정하기» — 정식 답변을 프리필한 작성 화면으로 (#582). */
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
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

    var questionExpanded by remember { mutableStateOf(true) }

    // 리스트 형에서 전체가 비었을 때만 캘린더 없이 빈 상태를 보여준다. 캘린더 형에서는
    // 빈 상태가 캘린더를 대체하면 월 이동 수단이 사라진다 (#724).
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
        // FAB 이 콘텐츠 위에 뜨므로 목록이 스스로 그 자리를 비운다 — 안 그러면 마지막
        // 항목이 가려지고, 스크롤이 없을 만큼 항목이 적으면 볼 방법이 없다 (#1713).
        contentPadding = PaddingValues(bottom = AfternoteFabContentBottomPadding),
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
            DailyQuestionListCard(
                answer = answer,
                onClick = { onItemClick(answer.id, yearMonth) },
                onEdit = { onEdit(answer.id) },
                onDelete = { onDelete(answer.id) },
            )
        }
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
            onDelete = {},
            onEdit = {},
            onItemClick = { _, _ -> },
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
            onDelete = {},
            onEdit = {},
            onItemClick = { _, _ -> },
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
