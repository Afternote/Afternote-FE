package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray4
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DeepThoughtCard
import com.afternote.feature.mindrecord.presentation.component.FlowTags
import com.afternote.feature.mindrecord.presentation.component.Legend
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import com.afternote.feature.mindrecord.presentation.model.Tag
import java.time.LocalDate

@Composable
fun DeepThoughtScreen(modifier: Modifier = Modifier) {
    var isListView by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf<String>("전체 카테고리", "카테고리", "카테고리", "카테고리")
    var selectedTag by remember { mutableStateOf<Tag?>(null) }
    val tags =
        listOf<Tag>(
            Tag("성장", 1),
            Tag("배움", 2),
            Tag("도전", 3),
            Tag("성장", 4),
            Tag("성장", 5),
        )

    val deepThoughtList =
        listOf<DeepThoughtModel>(
            DeepThoughtModel(
                title = "진정한 행복의 의미",
                date = LocalDate.now(),
                tag = listOf(Tag("행복", 1), Tag("희망", 2)),
                content = "큰 성취보다 ~~~",
            ),
            DeepThoughtModel(
                title = "진정한 행복의 의미",
                date = LocalDate.now(),
                tag = listOf(Tag("행복", 1), Tag("희망", 2)),
                content = "큰 성취보다 ~~~",
            ),
            DeepThoughtModel(
                title = "진정한 행복의 의미",
                date = LocalDate.now(),
                tag = listOf(Tag("행복", 1), Tag("희망", 2)),
                content = "큰 성취보다 ~~~",
            ),
            DeepThoughtModel(
                title = "진정한 행복의 의미",
                date = LocalDate.now(),
                tag = listOf(Tag("행복", 1), Tag("희망", 2)),
                content = "큰 성취보다 ~~~",
            ),
            DeepThoughtModel(
                title = "진정한 행복의 의미",
                date = LocalDate.now(),
                tag = listOf(Tag("행복", 1), Tag("희망", 2)),
                content = "큰 성취보다 ~~~",
            ),
        )
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "깊은 생각",
                onBackClick = {},
                action = {
                    ViewModeSwitcher(
                        image1 = R.drawable.core_ui_list,
                        image2 = R.drawable.core_ui_calendar,
                        onViewChange = { isListView = it },
                        isListView = isListView,
                    )
                },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        if (isListView) {
            Column(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .padding(18.dp),
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
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedIndex == index) Color(0xFF1F1F1F) else Gray4,
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(com.afternote.feature.mindrecord.presentation.R.drawable.mindrecord_mark),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "TAGS",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color(0xFF000000).copy(alpha = 0.4f),
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                FlowTags(
                    tags = tags,
                    selectedTag = selectedTag,
                    onclick = { selectedTag = null },
                    onTagClick = { selectedTag = it },
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(deepThoughtList) {
                        DeepThoughtCard(it)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .padding(18.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.clickable {},
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "2026년 3월",
                            color = Gray9,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Icon(
                            painter = painterResource(R.drawable.core_ui_arrowdown),
                            contentDescription = null,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                item {
                    Text(
                        text = "18개의 답변 완료",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF000000).copy(alpha = 0.35f),
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                }
                item {
                    DailyCalendar(
                        year = 2026,
                        month = 3,
                        type = MindRecordCategory.DEEP_THOUGHT,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Legend()
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "DAILY ANSWER",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color(0xFF000000).copy(alpha = 0.4f),
                        )

                        HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(deepThoughtList) {
                    DeepThoughtCard(it)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepThoughtScreenPreview() {
    AfternoteTheme {
        DeepThoughtScreen()
    }
}
