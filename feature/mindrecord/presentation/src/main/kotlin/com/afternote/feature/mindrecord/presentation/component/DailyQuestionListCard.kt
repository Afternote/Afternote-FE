package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import java.time.LocalDate

@Composable
fun DailyQuestionListCard(
    answer: DailyQuestion,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFFFFFF),
            ),
        border = BorderStroke(1.dp, color = Color(0xFF000000).copy(alpha = 0.05f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = answer.date.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    color = AfternoteDesign.colors.gray6,
                )

                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mindrecord_horizontal),
                        tint = AfternoteDesign.colors.gray5,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Text(
                text = answer.title,
                style = MaterialTheme.typography.bodySmall,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = answer.content,
                style = MaterialTheme.typography.displayMedium,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionCardPreview() {
    AfternoteTheme {
        DailyQuestionListCard(
            answer =
                DailyQuestion(
                    title = "test",
                    content = "test for DailyQuestionCardPreview",
                    date = LocalDate.now(),
                ),
        )
    }
}
