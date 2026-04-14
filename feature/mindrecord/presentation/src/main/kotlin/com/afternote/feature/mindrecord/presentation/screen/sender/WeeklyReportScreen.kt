package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.EmotionKeywordCard
import com.afternote.feature.mindrecord.presentation.component.InsightCard
import com.afternote.feature.mindrecord.presentation.component.WeeklyMoodCalendar
import com.afternote.feature.mindrecord.presentation.component.WeeklyReportReviewCard
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import java.time.LocalDate

@Composable
fun WeeklyReportScreen(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        item {
            WeeklyReportReviewCard()
        }

        item {
            Text(
                text =
                    buildAnnotatedString {
                        withStyle(
                            style = MaterialTheme.typography.titleMedium.toSpanStyle(),
                        ) {
                            append("이번 주, ")
                            withStyle(
                                style =
                                    MaterialTheme.typography.titleMedium
                                        .copy(color = AfternoteDesign.colors.b1)
                                        .toSpanStyle(),
                            ) {
                                append("박서연")
                            }
                            append("님은 ")
                            withStyle(
                                style =
                                    MaterialTheme.typography.titleMedium
                                        .copy(color = AfternoteDesign.colors.b1)
                                        .toSpanStyle(),
                            ) {
                                append("3일")
                            }
                            append("의 마음을 기록하셨네요.")
                        }
                    },
            )
        }

        item {
            WeeklyMoodCalendar()
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TOP KEYWORDS",
                    style = AfternoteDesign.typography.mono,
                    color = Color(0xFF000000).copy(alpha = 0.4f),
                )

                HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            EmotionKeywordCard()
        }
        item {
            InsightCard()
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "HISTORY",
                    style = AfternoteDesign.typography.mono,
                    color = Color(0xFF000000).copy(alpha = 0.4f),
                )

                HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            DailyQuestionListCard(
                answer =
                    DailyQuestion(
                        title = "오늘하루",
                        date = LocalDate.now(),
                        content = "오늘 하루",
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyReportScreenPreview() {
    AfternoteTheme {
        WeeklyReportScreen()
    }
}
