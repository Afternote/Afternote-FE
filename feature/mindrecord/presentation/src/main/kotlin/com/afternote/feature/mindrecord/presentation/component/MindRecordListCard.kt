package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray5
import com.afternote.core.ui.theme.Gray7
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory

@Composable
fun MindRecordListCard(
    modifier: Modifier = Modifier,
    category: MindRecordCategory,
) {
    Box(modifier = modifier.border(1.dp, color = Gray2, shape = RoundedCornerShape(6.dp))) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 16.dp, bottom = 15.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF000000).copy(alpha = 0.05f),
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(category.imageUrl),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Gray7,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = category.description,
                style = MaterialTheme.typography.displayMedium,
                color = Gray5,
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "18",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MindRecordListCardPreviewDIARY() {
    AfternoteTheme {
        MindRecordListCard(
            category = MindRecordCategory.DIARY,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MindRecordListCardPreviewDAILYQUESTION() {
    AfternoteTheme {
        MindRecordListCard(
            category = MindRecordCategory.DAILY_QUESTION,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MindRecordListCardPreviewDEEPTHOUGHT() {
    AfternoteTheme {
        MindRecordListCard(
            category = MindRecordCategory.DEEP_THOUGHT,
        )
    }
}
