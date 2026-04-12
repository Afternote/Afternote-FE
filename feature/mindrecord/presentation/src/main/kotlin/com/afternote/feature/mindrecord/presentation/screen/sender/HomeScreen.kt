package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.HomeTopBar
import com.afternote.feature.mindrecord.presentation.component.MemoriesCard
import com.afternote.feature.mindrecord.presentation.component.MindRecordListCard
import com.afternote.feature.mindrecord.presentation.component.TodayQuestionCard
import com.afternote.feature.mindrecord.presentation.component.WeeklyReportCard
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { HomeTopBar() },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 22.5.dp),
        ) {
            item {
                Text(
                    text = "나의 기록",
                    style = AfternoteDesign.typography.h1,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "오늘도 당신의 하루를 차분히 기록해보세요.",
                    style = AfternoteDesign.typography.captionLargeR,
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                TodayQuestionCard(modifier = Modifier.height(230.dp))

                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MindRecordCategory.entries.forEach { recordCategory ->
                        MindRecordListCard(
                            modifier = Modifier.size(110.dp, 180.dp),
                            category = recordCategory,
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "WEEK REPORT",
                        style = AfternoteDesign.typography.mono,
                        color = Color(0xFF000000).copy(alpha = 0.4f),
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                WeeklyReportCard(modifier = Modifier.height(150.dp))
                Spacer(modifier = Modifier.height(40.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "MEMORIES",
                        style = AfternoteDesign.typography.mono,
                        color = Color(0xFF000000).copy(alpha = 0.4f),
                    )

                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                MemoriesCard()
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
