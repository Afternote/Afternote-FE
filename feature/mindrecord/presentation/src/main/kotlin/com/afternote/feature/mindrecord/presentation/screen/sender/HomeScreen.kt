package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.afternote.core.ui.R
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.button.FAB.AfternoteFloatingActionButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.TitleTopBar
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.DailyQuestionListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.DiaryListViewModel
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.YearMonth
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onWriteClick: (MindRecordCategoryUi) -> Unit,
    // 목록 항목 탭 → 상세(열람) 화면. (기록 ID, 일기 여부, 목록이 보고 있던 달) (#759).
    onRecordClick: (Long, Boolean, YearMonth) -> Unit,
    // 목록의 «수정하기» → 프리필한 작성 화면 (#582).
    onEditDailyQuestion: (Long) -> Unit,
    onEditDiary: (Long, YearMonth) -> Unit,
) {
    // Figma 2757:16116 — 마음의 기록 탭은 데일리 질문 / 일기 / 주간리포트 3개
    val categories =
        remember {
            listOf(
                MindRecordCategoryUi.DailyQuestion,
                MindRecordCategoryUi.Diary,
                MindRecordCategoryUi.WeeklyReport,
            )
        }

    // 선택 상태의 단일 출처는 pager 다. 종전에는 selectedIndex 로 애니메이션을 시작하면서
    // pagerState.currentPage 를 다시 selectedIndex 에 기록해, 0↔2 전환 중 거쳐 가는 1번
    // 페이지가 선택값을 덮어써 가운데 탭(일기)에 멈췄다 (#722).
    //
    // 탭 클릭은 pager 에게 "거기로 가라" 고만 말하고, 화면이 읽는 값은 pager 의
    // settledPage 하나뿐이다 — 스크롤 도중의 중간 페이지가 선택으로 굳지 않는다.
    val pagerState = rememberPagerState { categories.size }
    val selectedIndex = pagerState.settledPage
    val selectedCategory = categories[selectedIndex]
    val scope = rememberCoroutineScope()

    // 탭마다 따로 기억한다. 종전에는 한 값을 두 탭이 공유했는데 의미가 서로 반대였다 —
    // 데일리질문은 false 를 캘린더로, 일기는 같은 false 를 2열 그리드로 썼다. 그래서 탭을
    // 오가면 아이콘은 그대로인데 표시가 뒤바뀐 것처럼 보였다 (#724).
    var dailyQuestionListView by rememberSaveable { mutableStateOf(true) }
    var diaryListView by rememberSaveable { mutableStateOf(true) }

    // 탭 VM 은 **여기서 만들지 않는다.** 호이스팅하면 선택하지 않은 탭의 `init` 조회까지
    // 진입 즉시 나간다 — 마음의 기록 첫 진입 한 번에 요청이 7건 나간 가장 큰 원인이었다.
    // 특히 주간리포트는 서버가 같은 주차의 반복 GET 에도 Gemini 를 다시 호출하므로
    // (Afternote-BE#118), 열지도 않은 탭의 프리페치가 모델 호출 비용으로 이어진다 (#736).
    //
    // 대신 각 화면이 스스로 `ON_RESUME` 갱신을 건다 — 탭에 들어가야 컴포즈되므로
    // 그 시점이 곧 "처음 필요한 시점" 이다.

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleTopBar(
                title = stringResource(MindRecordR.string.mindrecord_home_title),
                actions = {
                    // 보기 상태를 탭마다 따로 기억하므로(#724) «어느 탭의 상태를 읽고 쓰는가» 가
                    // 갈린다. 종전에는 읽기와 쓰기가 서로 다른 `when` 에 있었고 둘 다 `else` 로
                    // 일기 상태를 가리켜, 주간리포트가 일기의 보기 상태를 빌려 쓰고 있었다 —
                    // 탭이 하나 늘면 그 빌림이 새 탭으로 조용히 이어진다. 한 `when` 으로 합쳐
                    // 탭이 늘면 컴파일이 막게 한다 (#1765).
                    //
                    // 스위처 호출은 한 자리로 남긴다. 갈래마다 따로 부르면 탭을 옮길 때 컴포지션
                    // 자리가 바뀌어 `animateDpAsState` 가 새로 시작되고, 표시가 미끄러지지 않고 튄다.
                    val viewMode =
                        when (selectedCategory) {
                            MindRecordCategoryUi.DailyQuestion -> {
                                ViewModeBinding(dailyQuestionListView) { dailyQuestionListView = it }
                            }

                            MindRecordCategoryUi.Diary -> {
                                ViewModeBinding(diaryListView) { diaryListView = it }
                            }

                            // 주간리포트는 리스트/캘린더 두 보기가 없다 — 스위치를 눌러도 화면이
                            // 바뀌지 않으니 아예 노출하지 않는다. 컨트롤과 동작을 맞춘다 (#723).
                            // 같은 이유로 아래 FAB 도 이 탭에서 숨긴다.
                            MindRecordCategoryUi.WeeklyReport -> {
                                null
                            }
                        }

                    if (viewMode != null) {
                        ViewModeSwitcher(
                            isListView = viewMode.isListView,
                            image1 = R.drawable.core_ui_list,
                            image2 = R.drawable.core_ui_calendar,
                            onViewChange = viewMode.onViewChange,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (selectedCategory != MindRecordCategoryUi.WeeklyReport) {
                AfternoteFloatingActionButton(
                    onClick = { onWriteClick(selectedCategory) },
                )
            }
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                // 지정하지 않으면 M3 baseline surface(#FEF7FF)가 나와 시안 배경(#FAFAFA)과 어긋난다.
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {},
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier =
                            Modifier.tabIndicatorOffset(
                                selectedIndex,
                                matchContentSize = false,
                            ),
                        width = 80.dp,
                        color = AfternoteDesign.colors.gray9,
                    )
                },
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedIndex == index,
                        // scrollToPage 는 중간 페이지를 거치지 않는다. animate 로 넘기면
                        // 0 → 2 이동이 1번을 지나며 그 화면이 컴포즈되고, hiltViewModel() 이
                        // VM 을 만들며 init 조회가 나간다 — 바로 아래 단(#736)이 «열지도 않은
                        // 탭의 조회를 없앤다» 로 줄여 둔 요청이 탭 이동마다 되살아난다.
                        onClick = { scope.launch { pagerState.scrollToPage(index) } },
                        text = {
                            Text(
                                text = stringResource(category.titleRes),
                                color = if (selectedIndex == index) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray4,
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // page 인자를 실제로 쓴다 — 종전에는 무시하고 전역 selectedCategory 만 렌더해
            // 스와이프 중에도 같은 화면이 보였다 (#722).
            HorizontalPager(state = pagerState) { page ->
                when (categories[page]) {
                    MindRecordCategoryUi.DailyQuestion -> {
                        DailyQuestionAnswerListScreen(
                            isListView = dailyQuestionListView,
                            onItemClick = { id, yearMonth -> onRecordClick(id, false, yearMonth) },
                            onEditClick = onEditDailyQuestion,
                        )
                    }

                    MindRecordCategoryUi.Diary -> {
                        DiaryScreen(
                            isListView = diaryListView,
                            onItemClick = { id, yearMonth -> onRecordClick(id, true, yearMonth) },
                            onEditClick = onEditDiary,
                        )
                    }

                    MindRecordCategoryUi.WeeklyReport -> {
                        WeeklyReportScreen()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    AfternoteTheme {
        HomeScreen(
            onEditDailyQuestion = {},
            onEditDiary = { _, _ -> },
            onRecordClick = { _, _, _ -> },
            onWriteClick = {},
        )
    }
}

/**
 * 보기 전환 스위처가 읽고 쓸 탭별 상태 한 쌍.
 *
 * 탭마다 상태를 따로 기억하지만(#724) 스위처 자체는 한 자리에서만 부르기 위한 묶음이다 —
 * 갈래마다 스위처를 따로 부르면 탭 이동이 컴포지션 자리를 바꿔 표시 애니메이션이 새로 시작된다.
 */
private class ViewModeBinding(
    val isListView: Boolean,
    val onViewChange: (Boolean) -> Unit,
)
