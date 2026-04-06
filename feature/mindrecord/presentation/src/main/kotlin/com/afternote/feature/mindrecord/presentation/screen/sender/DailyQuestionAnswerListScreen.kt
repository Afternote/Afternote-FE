package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.afternote.core.ui.R
import com.afternote.core.ui.ViewModeSwitcher
import com.afternote.core.ui.scaffold.AfternoteFab
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.Legend
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import java.time.LocalDate

@Composable
fun DailyQuestionAnswerListScreen(modifier: Modifier = Modifier) {
    var isListView by remember { mutableStateOf(false) }

    val testDailyQuestion =
        listOf(
            DailyQuestion(
                title = "test",
                content = "test for DailyQuestionCardPreview",
                date = LocalDate.now(),
            ),
            DailyQuestion(
                title = "test",
                content = "test for DailyQuestionCardPreview",
                date = LocalDate.now(),
            ),
            DailyQuestion(
                title = "test",
                content = "test for DailyQuestionCardPreview",
                date = LocalDate.now(),
            ),
            DailyQuestion(
                title = "test",
                content = "test for DailyQuestionCardPreview",
                date = LocalDate.now(),
            ),
            DailyQuestion(
                title = "test",
                content = "test for DailyQuestionCardPreview",
                date = LocalDate.now(),
            ),
            DailyQuestion(
                title = "test",
                content = "test for DailyQuestionCardPreview",
                date = LocalDate.now(),
            ),
        )
    Scaffold(
        topBar = {
            DetailTopBar(
                onBackClick = {},
                action = {
                    ViewModeSwitcher(
                        isListView = isListView,
                        image1 = R.drawable.core_ui_list,
                        image2 = R.drawable.core_ui_calendar,
                        onViewChange = { isListView = it },
                    )
                },
                title = "데일리 기록",
            )
        },
        floatingActionButton = {
            AfternoteFab(
                onClick = { },
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isListView) {
                items(testDailyQuestion) {
                    DailyQuestionListCard(answer = it)
                }
            } else {
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
                }
                item {
                    DailyCalendar(
                        year = 2026,
                        month = 3,
                        type = MindRecordCategory.DAILY_QUESTION,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Legend()
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
                items(testDailyQuestion) {
                    DailyQuestionListCard(answer = it)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionAnswerListScreenPreview() {
    AfternoteTheme {
        DailyQuestionAnswerListScreen()
    }
}
