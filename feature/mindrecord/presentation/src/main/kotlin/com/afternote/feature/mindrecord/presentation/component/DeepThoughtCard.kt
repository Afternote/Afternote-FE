package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.Tag
import java.time.LocalDate

@Composable
fun DeepThoughtCard(
    deepThought: DeepThoughtModel,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.mindrecord_bulb),
                        contentDescription = null,
                        tint = Color(0xFF000000).copy(alpha = 0.3f),
                    )
                    Text(
                        text = deepThought.date.toString(),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = Color(0xFF000000).copy(0.4f),
                    )
                }

                Box(
                    modifier = Modifier.clickable {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mindrecord_horizontal),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray5,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deepThought.title,
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deepThought.content,
                style = AfternoteDesign.typography.bodySmallR,
                color = Color(0xFF000000).copy(0.5f),
            )

            Spacer(modifier = Modifier.height(13.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deepThought.tag.forEach { tag ->
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(16777200.dp))
                                .background(AfternoteDesign.colors.gray2)
                                .padding(5.dp),
                    ) {
                        Text(
                            text = "#${tag.name}",
                            color = AfternoteDesign.colors.gray6,
                            style = AfternoteDesign.typography.mono,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeepThoughtCardPreview() {
    AfternoteTheme {
        DeepThoughtCard(
            deepThought =
                DeepThoughtModel(
                    title = "진정한 행복의 의미",
                    date = LocalDate.now(),
                    tag = listOf(Tag("행복", 1), Tag("희망", 2)),
                    content = "큰 성취보다 ~~~",
                ),
        )
    }
}
