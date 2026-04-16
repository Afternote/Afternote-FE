package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionWriteHeaderCard
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import java.time.LocalDate

@Composable
fun DailyQuestionAnswerListScreen(
    modifier: Modifier = Modifier,
    isListView: Boolean = true,
) {
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

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isListView) {
            item {
                DailyQuestionWriteHeaderCard()
            }
            items(testDailyQuestion) {
                DailyQuestionListCard(answer = it)
            }
        } else {
            item {
                DailyCalendar(
                    year = 2026,
                    month = 3,
                    type = MindRecordCategoryUi.DailyQuestion,
                    onPrevMonth = {},
                    onNextMonth = {},
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "DAILY ANSWER",
                        style = AfternoteDesign.typography.mono,
                        color = Color(0xFF000000).copy(alpha = 0.4f),
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                DailyQuestionWriteHeaderCard()
                // 나중에 vm 들어오면 변경
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionAnswerListScreenPreviewFalse() {
    AfternoteTheme {
        DailyQuestionAnswerListScreen(isListView = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionAnswerListScreenPreviewTrue() {
    AfternoteTheme {
        DailyQuestionAnswerListScreen(isListView = true)
    }
}
