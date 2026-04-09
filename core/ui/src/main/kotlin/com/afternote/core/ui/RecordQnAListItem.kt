package com.afternote.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * A reusable list item for displaying a record-like Q/A content with date.
 *
 * This is shared across multiple features (e.g., daily record and setting screens).
 */
@Composable
fun RecordQnAListItem(
    question: String,
    answer: String,
    dateText: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp),
        ) {
            Text(
                text = question,
                style =
                    AfternoteDesign.typography.textField.copy(
                        fontWeight = FontWeight.Medium,
                        color = AfternoteDesign.colors.gray9,
                    ),
                modifier = Modifier.padding(start = 4.dp),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 7.dp)
                        .defaultMinSize(minHeight = 88.dp)
                        .background(
                            color = AfternoteDesign.colors.gray2,
                            shape = RoundedCornerShape(8.dp),
                        ).padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(
                    text = answer,
                    style =
                        AfternoteDesign.typography.bodySmallR.copy(
                            color = AfternoteDesign.colors.gray8,
                        ),
                )
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = dateText,
                    style =
                        AfternoteDesign.typography.captionLargeR.copy(
                            fontWeight = FontWeight.Medium,
                            color = AfternoteDesign.colors.gray5,
                        ),
                )

                trailing?.invoke()
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            thickness = 1.dp,
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordQnAListItemPreview() {
    AfternoteTheme {
        RecordQnAListItem(
            question = "오늘 하루, 누구에게 가장 고마웠나요?",
            answer = "아무 말 없이 그저 나의 곁을 지켜주는 아내가 너무 고맙다.",
            dateText = "2025. 10. 09.",
        )
    }
}
