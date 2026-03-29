package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteFab
import com.afternote.core.ui.R
import com.afternote.core.ui.TopBar
import com.afternote.core.ui.component.ViewModeSwitcher
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DiaryCard
import com.afternote.feature.mindrecord.presentation.component.DiaryComponent
import com.afternote.feature.mindrecord.presentation.component.DiaryReportCard
import com.afternote.feature.mindrecord.presentation.component.WeeklyEmotionCard
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import java.time.LocalDate

@Composable
fun DiaryScreen(modifier: Modifier = Modifier) {
    var isListView by remember { mutableStateOf(false) }
    val testDiaryList =
        listOf(
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
            DailyDiary(
                title = "가족과 함께한 저녁 식사",
                content = "오랜만에 가족들과 testsetsetsetsetset",
                date = LocalDate.now(),
            ),
        )
    Scaffold(
        topBar = {
            TopBar(
                title = "일기",
                action = {
                    ViewModeSwitcher(
                        isListView = isListView,
                        image1 = R.drawable.core_ui_grid,
                        image2 = R.drawable.core_ui_calendar,
                        onViewChange = { isListView = it },
                    )
                },
                onBackClick = {},
            )
        },
        floatingActionButton = {
            AfternoteFab(
                onClick = {},
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        if (isListView) {
            LazyColumn(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
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

                    Spacer(modifier = Modifier.height(28.dp))
                }

                item {
                    DailyCalendar(
                        year = 2026,
                        month = 3,
                        type = MindRecordCategory.DIARY,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    WeeklyEmotionCard()

                    Spacer(modifier = Modifier.height(24.dp))
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

                items(testDiaryList) {
                    DiaryComponent(
                        diary = it,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp),
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(2) }) {
                    DiaryReportCard()
                    Spacer(modifier = Modifier.height(24.dp))
                }
                items(testDiaryList) {
                    DiaryCard(
                        diary = it,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryScreenPreview() {
    AfternoteTheme {
        DiaryScreen()
    }
}
