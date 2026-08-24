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
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onWriteClick: (MindRecordCategoryUi) -> Unit = {},
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

    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedCategory = categories[selectedIndex]

    // 탭마다 따로 기억한다. 종전에는 한 값을 두 탭이 공유했는데 의미가 서로 반대였다 —
    // 데일리질문은 false 를 캘린더로, 일기는 같은 false 를 2열 그리드로 썼다. 그래서 탭을
    // 오가면 아이콘은 그대로인데 표시가 뒤바뀐 것처럼 보였다 (#724).
    var dailyQuestionListView by rememberSaveable { mutableStateOf(true) }
    var diaryListView by rememberSaveable { mutableStateOf(true) }
    val isListView =
        when (selectedCategory) {
            MindRecordCategoryUi.DailyQuestion -> dailyQuestionListView
            else -> diaryListView
        }

    val pagerState = rememberPagerState { categories.size }
    LaunchedEffect(selectedIndex) {
        pagerState.animateScrollToPage(selectedIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }

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
                    // 주간리포트는 리스트/캘린더 두 보기가 없다 — 스위치를 눌러도 화면이
                    // 바뀌지 않으니 아예 노출하지 않는다. 컨트롤과 동작을 맞춘다 (#723).
                    // 같은 이유로 아래 FAB 도 이 탭에서 숨긴다.
                    if (selectedCategory != MindRecordCategoryUi.WeeklyReport) {
                        ViewModeSwitcher(
                            isListView = isListView,
                            image1 = R.drawable.core_ui_list,
                            image2 = R.drawable.core_ui_calendar,
                            onViewChange = { listView ->
                                when (selectedCategory) {
                                    MindRecordCategoryUi.DailyQuestion -> dailyQuestionListView = listView
                                    else -> diaryListView = listView
                                }
                            },
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
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
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
                        color = Color(0xFF1F1F1F),
                    )
                },
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
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

            HorizontalPager(state = pagerState) { _ ->
                when (selectedCategory) {
                    MindRecordCategoryUi.DailyQuestion -> {
                        DailyQuestionAnswerListScreen(isListView = dailyQuestionListView)
                    }

                    MindRecordCategoryUi.Diary -> {
                        DiaryScreen(isListView = diaryListView)
                    }

                    else -> {
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
        HomeScreen()
    }
}
