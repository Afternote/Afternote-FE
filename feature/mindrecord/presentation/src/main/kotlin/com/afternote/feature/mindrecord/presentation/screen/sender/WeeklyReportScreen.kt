package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.topbar.DetailTopBar
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.B1
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.component.EmotionKeywordCard
import com.afternote.feature.mindrecord.presentation.component.InsightCard
import com.afternote.feature.mindrecord.presentation.component.WeeklyMoodCalendar
import com.afternote.feature.mindrecord.presentation.component.WeeklyReportReviewCard
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import java.time.LocalDate

@Composable
fun WeeklyReportScreen(modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = "기록",
                onBackClick = {},
                action = {},
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            WeeklyReportReviewCard()

            buildAnnotatedString {
                withStyle(
                    style = MaterialTheme.typography.titleMedium.toSpanStyle(),
                ) {
                    append(
                        text = "이번 주,",
                    )
                    withStyle(
                        style =
                            MaterialTheme.typography.titleMedium
                                .copy(color = B1)
                                .toSpanStyle(),
                    ) {
                        append(
                            text = "박서연",
                        )
                    }
                    append(
                        text = "님은",
                    )
                    withStyle(
                        style =
                            MaterialTheme.typography.titleMedium
                                .copy(color = B1)
                                .toSpanStyle(),
                    ) {
                        append(
                            text = "3일",
                        )
                    }
                    append(
                        text = "의 마음을 기록하셨네요.",
                    )
                }
            }

            WeeklyMoodCalendar()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TOP KEYWORDS",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFF000000).copy(alpha = 0.4f),
                )

                HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            EmotionKeywordCard()

            InsightCard()

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "HISTORY",
                    style = MaterialTheme.typography.displaySmall,
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
