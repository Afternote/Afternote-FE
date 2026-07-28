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
import androidx.compose.runtime.rememberUpdatedState
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
    dailyQuestionViewModel: DailyQuestionListViewModel = hiltViewModel(),
    diaryViewModel: DiaryListViewModel = hiltViewModel(),
    // WeeklyReportScreen 과 같은 NavBackStackEntry 를 owner 로 쓰므로 동일 인스턴스다.
    // 여기서 호이스팅하면 주간리포트 탭에 들어가지 않아도 VM 의 init { load(...) } 가 돌아
    // 마음의 기록 탭 진입마다 요약 조회가 한 번 나간다. 대신 탭을 열면 스피너 없이 즉시 보이는
    // 프리페치가 되므로 트레이드오프를 받아들인다.
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
    //
    // 람다를 캡처하는 옵저버는 DisposableEffect(key) 안에서 한 번만 만들어지므로, 매 컴포지션
    // 새로 계산되는 지역 val 인 selectedCategory 를 그대로 읽으면 화면 진입 시점 값에 고정된다.
    // key 를 selectedCategory 로 주면 이미 RESUMED 인 상태에서 옵저버가 재등록되며 탭 전환마다
    // refresh 가 한 번 더 돌아 위 snapshotFlow 경로와 중복되므로, 최신 값 읽기로 해결한다.
    val currentCategory by rememberUpdatedState(selectedCategory)
    var isFirstResume by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (isFirstResume) {
            isFirstResume = false
        } else {
            when (currentCategory) {
                MindRecordCategoryUi.DailyQuestion -> dailyQuestionViewModel.refresh()
                MindRecordCategoryUi.Diary -> diaryViewModel.refresh()
                MindRecordCategoryUi.WeeklyReport -> weeklyReportViewModel.refresh()
            }
        }
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
