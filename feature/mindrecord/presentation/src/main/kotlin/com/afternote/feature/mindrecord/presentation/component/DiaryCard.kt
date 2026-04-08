package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import java.time.LocalDate

@Composable
fun DiaryCard(
    diary: DailyDiary,
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
        Column {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.mindrecord_img),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                )

                diary.emotion?.let {
                    Text(
                        text = it,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = diary.date.toString(),
                    style = AfternoteDesign.typography.footnoteCaption,
                    color = AfternoteDesign.colors.gray6,
                )

                Box(
                    modifier = Modifier.clickable {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mindrecord_horizontal),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray6,
                    )
                }
            }

            Text(
                text = diary.title,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(2.5.dp))
            Text(
                text = diary.content,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(17.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryCardPreview() {
    AfternoteTheme {
        DiaryCard(
            diary =
                DailyDiary(
                    title = "가족과 함께한 저녁 식사",
                    content = "오랜만에 가족들과 testsetsetsetsetset",
                    date = LocalDate.now(),
                    emotion = "\uD83D\uDE0A",
                ),
        )
    }
}
