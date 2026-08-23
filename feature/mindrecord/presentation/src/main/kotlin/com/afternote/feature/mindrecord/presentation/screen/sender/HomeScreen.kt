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
    onEditDailyQuestion: (Long) -> Unit = {},
    onEditDiary: (Long) -> Unit = {},
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

    // 탭 전환과 ON_RESUME 은 성격이 같은 자동 갱신이므로 대상도 같아야 한다.
    // 한쪽에만 주간리포트가 빠지면, 일기를 쓰고 돌아와 주간리포트 탭으로 넘겼을 때
    // 방금 쓴 일기가 주간 집계에 잡히지 않는다.
    val refreshTab: (MindRecordCategoryUi) -> Unit = { category ->
        when (category) {
            MindRecordCategoryUi.DailyQuestion -> dailyQuestionViewModel.refreshOnReturn()
            MindRecordCategoryUi.Diary -> diaryViewModel.refreshOnReturn()
            MindRecordCategoryUi.WeeklyReport -> weeklyReportViewModel.refreshOnReturn()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { selectedIndex }
            .drop(1)
            .collect { index -> refreshTab(categories[index]) }
    }

    // ON_RESUME 은 작성 화면 복귀뿐 아니라 화면 off/on · 홈 버튼 후 복귀 · 권한 다이얼로그
    // 닫기에서도 발화한다. 화면이 살아 있는 채로 발화하므로 refreshOnReturn() 을 쓴다 —
    // 로딩을 방출하지 않아 캘린더 월·스크롤 위치가 살아남고, 실패해도 보고 있던 화면을 유지한다.
    //
    // 최초 진입의 중복 호출은 각 VM 이 진행 중인 로드 Job 으로 막는다. 컴포지션 쪽 플래그로
    // 막으면 수명이 어긋난다 — rememberSaveable 은 SavedState 수명이고 막으려는 init { load() }
    // 는 ViewModelStore 수명이라, 프로세스 사망 후 복원에서 플래그는 false 로 살아 돌아오는데
    // VM 은 새로 만들어져 같은 조회가 두 번 나간다.
    //
    // selectedCategory 는 State 가 아니라 매 컴포지션 새로 계산되는 지역 val 이지만 여기서는
    // 최신 값이 읽힌다 — LifecycleEventEffect 가 onEvent 를 rememberUpdatedState 로 감싸
    // 옵저버에 넘기기 때문이다 (lifecycle-runtime-compose 2.11.0 LifecycleEffect.kt:66).
    // LifecycleResumeEffect 는 effects 람다를 DisposableEffect(lifecycleOwner, scope) 안에서
    // 직접 캡처해 진입 시점 값에 고정되므로, 이 화면에서는 쓰면 안 된다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshTab(selectedCategory)
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
                    MindRecordCategoryUi.DailyQuestion -> {
                        DailyQuestionAnswerListScreen(
                            isListView = isListView,
                            onEditClick = onEditDailyQuestion,
                        )
                    }

                    MindRecordCategoryUi.Diary -> {
                        DiaryScreen(
                            isListView = isListView,
                            onEditClick = onEditDiary,
                        )
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
