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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.afternote.feature.mindrecord.presentation.component.DiaryCard
import com.afternote.feature.mindrecord.presentation.component.DiaryComponent
import com.afternote.feature.mindrecord.presentation.component.DiaryReportCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.mapper.toEmoji
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListViewModel
import java.time.YearMonth
import androidx.compose.foundation.lazy.staggeredgrid.items as gridItems

private val PreviewYearMonth = YearMonth.of(2026, 7)

@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    /**
     * «수정하기» — 기록 ID 와 **보고 있는 달**. 달을 빼면 프리필이 이번 달 목록에서 그
     * 기록을 찾다 실패하고, 빈 화면에서 저장하면 원본을 덮어쓸 수 있다 (#582 리뷰).
     */
    onEditClick: (Long, YearMonth) -> Unit = { _, _ -> },
    viewModel: DiaryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        DiaryListUiState.Loading -> {
            LoadingBox(modifier)
        }

        is DiaryListUiState.Error -> {
            ErrorBox(message = state.message.asString(), modifier = modifier)
        }

        is DiaryListUiState.Success -> {
            DiaryListContent(
                modifier = modifier,
                isListView = isListView,
                diaries = state.diaries,
                yearMonth = state.yearMonth,
                monthDiaryCount = state.monthDiaryCount,
                weeklyMoodEmoji = state.weeklyDominantMood?.toEmoji(),
                onEdit = onEditClick,
                onDelete = viewModel::delete,
                onYearMonthChanged = viewModel::selectYearMonth,
            )
        }
    }
}

@Composable
internal fun DiaryListContent(
    isListView: Boolean,
    diaries: List<DailyDiary>,
    // 조회 중인 월은 VM 이 들고 있다 — 자동 갱신이 같은 월을 다시 조회해야 하고,
    // 로컬 remember 로 두면 로딩으로 이 컴포저블이 폐기될 때 함께 사라진다.
    // 기본값을 두지 않는다 — 빠뜨리면 조용히 이번 달로 돌아간다.
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
    monthDiaryCount: Int = 0,
    weeklyMoodEmoji: String? = null,
    /** «수정하기» — 기록 ID 와 이 화면이 보고 있는 달. 달은 여기서만 알 수 있다. */
    onEdit: (Long, YearMonth) -> Unit = { _, _ -> },
    onDelete: (Long) -> Unit = {},
    onYearMonthChanged: (YearMonth) -> Unit = {},
) {
    if (isListView && diaries.isEmpty()) {
        MindRecordEmptyState(
            modifier = modifier,
            title = stringResource(R.string.mindrecord_diary_empty_state_title),
            description = stringResource(R.string.mindrecord_diary_empty_state_description),
        )
        return
    }

    val currentMonthDiaries =
        diaries.filter { it.date.year == yearMonth.year && it.date.monthValue == yearMonth.monthValue }
    val answeredDays = currentMonthDiaries.map { it.date.dayOfMonth }.toSet()
    val emotionByDay =
        currentMonthDiaries
            .mapNotNull { diary -> diary.emotion?.let { diary.date.dayOfMonth to it } }
            .toMap()

    if (isListView) {
        LazyColumn(modifier = modifier) {
            item {
                DailyCalendar(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    type = MindRecordCategoryUi.Diary,
                    onPrevMonth = { onYearMonthChanged(yearMonth.minusMonths(1)) },
                    onNextMonth = { onYearMonthChanged(yearMonth.plusMonths(1)) },
                    answeredDays = answeredDays,
                    emotionByDay = emotionByDay,
                )
                Spacer(modifier = Modifier.height(10.dp))
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

            items(diaries, key = { it.id }) { diary ->
                DiaryComponent(
                    onEdit = { onEdit(diary.id, yearMonth) },
                    diary = diary,
                    modifier = Modifier.padding(vertical = 8.dp),
                    onDelete = { onDelete(diary.id) },
                )
            }
        }
    } else {
        // Figma 2671:16732 — 일기 카드 형: 리포트 카드 + 2열 masonry 그리드
        LazyVerticalStaggeredGrid(
            modifier = modifier,
            columns = StaggeredGridCells.Fixed(2),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                DiaryReportCard(
                    monthDiaryCount = monthDiaryCount,
                    weeklyMoodEmoji = weeklyMoodEmoji,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            gridItems(diaries, key = { it.id }) { diary ->
                DiaryCard(
                    onEdit = { onEdit(diary.id, yearMonth) },
                    diary = diary,
                    onDelete = { onDelete(diary.id) },
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
private fun DiaryScreenPreviewTrue() {
    AfternoteTheme {
        DiaryListContent(
            modifier = Modifier,
            isListView = true,
            diaries = emptyList(),
            // 프리뷰는 고정 월로 렌더한다 — YearMonth.now() 면 달이 바뀔 때마다 결과가 달라진다.
            yearMonth = PreviewYearMonth,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryScreenPreviewFalse() {
    AfternoteTheme {
        DiaryListContent(
            modifier = Modifier,
            isListView = false,
            diaries = emptyList(),
            yearMonth = PreviewYearMonth,
        )
    }
}
