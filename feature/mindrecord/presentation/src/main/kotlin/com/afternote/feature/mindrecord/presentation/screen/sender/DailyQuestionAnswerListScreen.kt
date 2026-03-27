package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.TopBar
import com.afternote.core.ui.component.FAB
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyQuestionListCard
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import java.time.LocalDate

@Composable
fun DailyQuestionAnswerListScreen(modifier: Modifier = Modifier) {
    var isListView by remember { mutableStateOf(true) }

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
            TopBar(
                onBackClick = {},
                onToggleClick = { isListView = !isListView},
                isListView = isListView,
            )
        },
        floatingActionButton = {
            FAB {
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.padding(paddingValues).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isListView) {
                items(testDailyQuestion) {
                    DailyQuestionListCard(answer = it)
                }
            } else {

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
