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
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems

@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
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
                monthDiaryCount = state.monthDiaryCount,
                weeklyMoodEmoji = state.weeklyDominantMood?.toEmoji(),
                onDelete = viewModel::delete,
                onYearMonthChanged = viewModel::refresh,
            )
        }
    }
}

@Composable
private fun DiaryListContent(
    isListView: Boolean,
    diaries: List<DailyDiary>,
    modifier: Modifier = Modifier,
    monthDiaryCount: Int = 0,
    weeklyMoodEmoji: String? = null,
    onDelete: (Long) -> Unit = {},
    onYearMonthChanged: (YearMonth) -> Unit = {},
) {
    if (isListView && diaries.isEmpty()) {
        MindRecordEmptyState(modifier = modifier)
        return
    }

    var yearMonth by remember { mutableStateOf(YearMonth.now()) }
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
                    onPrevMonth = {
                        yearMonth = yearMonth.minusMonths(1)
                        onYearMonthChanged(yearMonth)
                    },
                    onNextMonth = {
                        yearMonth = yearMonth.plusMonths(1)
                        onYearMonthChanged(yearMonth)
                    },
                    answeredDays = answeredDays,
                    emotionByDay = emotionByDay,
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Figma 110:16804 — DAILY ANSWER 구분 텍스트 + gray3 라인
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DAILY ANSWER",
                        style = AfternoteDesign.typography.mono,
                        color = AfternoteDesign.colors.gray6,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 12.dp),
                        color = AfternoteDesign.colors.gray3,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(diaries, key = { it.id }) { diary ->
                DiaryComponent(
                    diary = diary,
                    modifier = Modifier.padding(vertical = 8.dp),
                    onDelete = { onDelete(diary.id) },
                )
            }
        }
    } else {
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
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            staggeredItems(diaries, key = { it.id }) { diary ->
                DiaryCard(
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
        )
    }
}
