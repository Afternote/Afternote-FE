package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.scaffold.topbar.HomeTopBar
import com.afternote.core.ui.scaffold.topbar.TitleTopBar
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionWriteHeaderCard
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordListCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.component.WeeklyReportCard
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var isListView by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedCategory = MindRecordCategory.entries[selectedIndex]

    val pagerState = rememberPagerState { MindRecordCategory.entries.size }
    LaunchedEffect(selectedIndex) {
        pagerState.animateScrollToPage(selectedIndex)
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedIndex = pagerState.currentPage
    }
    Scaffold(
        modifier = modifier,
        topBar = { TitleTopBar(
            title = "마음의 기록",
            actions = {
                ViewModeSwitcher(
                    isListView = isListView,
                    image1 = R.drawable.core_ui_list,
                    image2 = R.drawable.core_ui_calendar,
                    onViewChange = { isListView = it },
                )
            },
        ) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(horizontal = 20.dp)){
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
                MindRecordCategory.entries.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        text = {
                            Text(
                                text = category.title,
                                color = if (selectedIndex == index) Color(0xFF1F1F1F) else AfternoteDesign.colors.gray4,
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            HorizontalPager(state = pagerState) { page ->
                when (MindRecordCategory.entries[page]) {
                    MindRecordCategory.DAILY_QUESTION -> DailyQuestionAnswerListScreen(isListView = isListView)
                    MindRecordCategory.DIARY -> DiaryScreen(isListView = isListView)
                    MindRecordCategory.DEEP_THOUGHT -> DeepThoughtScreen(isListView = isListView)
                    MindRecordCategory.WEEKLY_REPORT -> WeeklyReportScreen()
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
