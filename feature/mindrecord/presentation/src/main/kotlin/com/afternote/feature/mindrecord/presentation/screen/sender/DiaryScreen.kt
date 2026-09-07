package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteSectionHeader
import com.afternote.core.ui.asString
import com.afternote.core.ui.button.FAB.AfternoteFabContentBottomPadding
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DiaryCard
import com.afternote.feature.mindrecord.presentation.component.DiaryComponent
import com.afternote.feature.mindrecord.presentation.component.DiaryReportCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordEmptyState
import com.afternote.feature.mindrecord.presentation.component.MindRecordErrorBox
import com.afternote.feature.mindrecord.presentation.mapper.toEmoji
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListUiState
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListViewModel
import java.time.YearMonth
import androidx.compose.foundation.lazy.staggeredgrid.items as gridItems

private val PreviewYearMonth = YearMonth.of(2026, 7)

/**
 * @param onItemClick 카드를 눌렀을 때 상세로 보낼 기록 ID 와 **보고 있는 달**. 달을 함께
 *   싣지 않으면 상세가 이번 달 목록에서 그 기록을 찾다 실패한다 — 목록은 달을 바꿀 수
 *   있으므로 지난달 기록이 통째로 열리지 않는다 (#759 리뷰). 기본값을 두지 않는다 —
 *   기본값이 있으면 호출부에서 빠뜨려도 컴파일이 되고, 카드가 눌리지 않는 채로 나간다 (#759).
 */
@Composable
fun DiaryScreen(
    onItemClick: (Long, YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
    /**
     * «수정하기» — 기록 ID 와 **보고 있는 달**. 달을 빼면 프리필이 이번 달 목록에서 그
     * 기록을 찾다 실패하고, 빈 화면에서 저장하면 원본을 덮어쓸 수 있다 (#582 리뷰).
     */
    onEditClick: (Long, YearMonth) -> Unit,
    viewModel: DiaryListViewModel = hiltViewModel(),
) {
    // 갱신을 이 화면이 직접 건다. HomeScreen 이 VM 을 호이스팅해 대신 걸어 주면, 탭에
    // 들어가지 않아도 VM 이 만들어져 `init` 조회가 미리 나간다 (#736).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        DiaryListUiState.Loading -> {
            LoadingBody(modifier)
        }

        is DiaryListUiState.Error -> {
            MindRecordErrorBox(
                message = state.message.asString(),
                onRetry = viewModel::retry,
                modifier = modifier,
            )
        }

        is DiaryListUiState.Success -> {
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
                DiaryListContent(
                    isListView = isListView,
                    diaries = state.diaries,
                    yearMonth = state.yearMonth,
                    monthDiaryCount = state.monthDiaryCount,
                    weeklyMoodEmoji = state.weeklyDominantMood?.toEmoji(),
                    onItemClick = onItemClick,
                    onEdit = onEditClick,
                    onDelete = viewModel::delete,
                    onYearMonthChanged = viewModel::selectYearMonth,
                )
            }
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
    /** 항목 탭 — 저장된 기록 본문을 여는 상세 화면 (#759). */
    onItemClick: (Long, YearMonth) -> Unit,
    /** «수정하기» — 기록 ID 와 이 화면이 보고 있는 달. 달은 여기서만 알 수 있다 (#582). */
    onEdit: (Long, YearMonth) -> Unit,
    onDelete: (Long) -> Unit,
    onYearMonthChanged: (YearMonth) -> Unit,
) {
    // 기록이 없다고 조기 반환하지 않는다. 종전에는 빈 상태가 캘린더를 통째로 대체해
    // 월 이동 버튼까지 사라졌고, 기록이 있는 달로 돌아갈 방법이 없었다 (#724).
    var selectedDay by remember(yearMonth) { mutableStateOf<Int?>(null) }

    val currentMonthDiaries =
        diaries.filter { it.date.year == yearMonth.year && it.date.monthValue == yearMonth.monthValue }
    val answeredDays = currentMonthDiaries.map { it.date.dayOfMonth }.toSet()
    val emotionByDay =
        currentMonthDiaries
            .mapNotNull { diary -> diary.emotion?.let { diary.date.dayOfMonth to it } }
            .toMap()

    // 날짜를 고르면 그 날 기록만, 안 고르면 그 달 전체를 보여준다.
    val visibleDiaries =
        selectedDay?.let { day -> currentMonthDiaries.filter { it.date.dayOfMonth == day } }
            ?: currentMonthDiaries

    if (isListView) {
        LazyColumn(
            modifier = modifier,
            // FAB 이 콘텐츠 위에 뜨므로 목록이 스스로 그 자리를 비운다 — 안 그러면 마지막
            // 항목이 가려지고, 스크롤이 없을 만큼 항목이 적으면 볼 방법이 없다 (#1713).
            contentPadding = PaddingValues(bottom = AfternoteFabContentBottomPadding),
        ) {
            item {
                DailyCalendar(
                    year = yearMonth.year,
                    month = yearMonth.monthValue,
                    type = MindRecordCategoryUi.Diary,
                    onPrevMonth = { onYearMonthChanged(yearMonth.minusMonths(1)) },
                    onNextMonth = { onYearMonthChanged(yearMonth.plusMonths(1)) },
                    answeredDays = answeredDays,
                    emotionByDay = emotionByDay,
                    selectedDay = selectedDay,
                    // 같은 날을 다시 누르면 선택을 푼다 — 그 달 전체로 돌아올 수단이 필요하다.
                    onDayClick = { day -> selectedDay = if (selectedDay == day) null else day },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                // core 정본을 쓴다. 종전에는 같은 구조를 인라인으로 다시 적어, 색이
                // black.copy(0.4f)·M3 baseline divider 로 토큰에서 벗어나 있었다 (#634).
                // 일기 목록인데 데일리질문 헤더가 붙어 있었고, 문자열도 코드 리터럴이었다 (#1712).
                AfternoteSectionHeader(title = stringResource(R.string.mindrecord_diary_list_section_header))
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (visibleDiaries.isEmpty()) {
                item {
                    MindRecordEmptyState(
                        title = stringResource(R.string.mindrecord_diary_empty_state_title),
                        description = stringResource(R.string.mindrecord_diary_empty_state_description),
                    )
                }
            }

            items(visibleDiaries, key = { it.id }) { diary ->
                DiaryComponent(
                    onEdit = { onEdit(diary.id, yearMonth) },
                    diary = diary,
                    onClick = { onItemClick(diary.id, yearMonth) },
                    modifier = Modifier.padding(vertical = 8.dp),
                    onDelete = { onDelete(diary.id) },
                )
            }
        }
    } else {
        // Figma 2671:16732 — 일기 카드 형: 리포트 카드 + 2열 masonry 그리드
        LazyVerticalStaggeredGrid(
            // FAB 이 콘텐츠 위에 뜨므로 목록이 스스로 그 자리를 비운다 — 안 그러면 마지막
            // 항목이 가려지고, 스크롤이 없을 만큼 항목이 적으면 볼 방법이 없다 (#1713).
            contentPadding = PaddingValues(bottom = AfternoteFabContentBottomPadding),
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
                    onClick = { onItemClick(diary.id, yearMonth) },
                    onDelete = { onDelete(diary.id) },
                )
            }
        }
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
            onDelete = {},
            onEdit = { _, _ -> },
            onItemClick = { _, _ -> },
            onYearMonthChanged = {},
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
            onDelete = {},
            onEdit = { _, _ -> },
            onItemClick = { _, _ -> },
            onYearMonthChanged = {},
        )
    }
}
