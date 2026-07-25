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
import androidx.lifecycle.compose.LifecycleResumeEffect
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
    dailyQuestionViewModel: DailyQuestionListViewModel = hiltViewModel(),
    diaryViewModel: DiaryListViewModel = hiltViewModel(),
    weeklyReportViewModel: WeeklyReportViewModel = hiltViewModel(),
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

    var isListView by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedCategory = categories[selectedIndex]

    val pagerState = rememberPagerState { categories.size }
    LaunchedEffect(selectedIndex) {
        pagerState.animateScrollToPage(selectedIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }

    LaunchedEffect(Unit) {
        snapshotFlow { selectedIndex }
            .drop(1)
            .collect { index ->
                when (categories[index]) {
                    MindRecordCategoryUi.DailyQuestion -> dailyQuestionViewModel.refresh()
                    MindRecordCategoryUi.Diary -> diaryViewModel.refresh()
                    else -> Unit
                }
            }
    }

    // 작성 화면(DailyQuestionWriteRoute/DiaryWriteRoute) 복귀 시 현재 탭 목록 refresh.
    // 최초 진입은 각 VM 의 init { load() } 가 이미 로드하므로 첫 resume 은 스킵한다.
    // (rememberSaveable: 작성 화면 이동으로 컴포지션에서 벗어나도 스킵 플래그가 초기화되지 않도록)
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        if (isFirstResume) {
            isFirstResume = false
        } else {
            when (selectedCategory) {
                MindRecordCategoryUi.DailyQuestion -> dailyQuestionViewModel.refresh()
                MindRecordCategoryUi.Diary -> diaryViewModel.refresh()
                MindRecordCategoryUi.WeeklyReport -> weeklyReportViewModel.refresh()
            }
        }
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TitleTopBar(
                title = stringResource(MindRecordR.string.mindrecord_home_title),
                actions = {
                    ViewModeSwitcher(
                        isListView = isListView,
                        image1 = R.drawable.core_ui_list,
                        image2 = R.drawable.core_ui_calendar,
                        onViewChange = { isListView = it },
                    )
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
                    MindRecordCategoryUi.DailyQuestion -> DailyQuestionAnswerListScreen(isListView = isListView)
                    MindRecordCategoryUi.Diary -> DiaryScreen(isListView = isListView)
                    else -> WeeklyReportScreen()
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
