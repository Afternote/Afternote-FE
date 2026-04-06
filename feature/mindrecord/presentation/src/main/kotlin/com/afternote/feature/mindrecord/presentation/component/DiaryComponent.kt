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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import java.time.LocalDate

@Composable
fun DiaryComponent(
    diary: DailyDiary,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFFFFFF),
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.mindrecord_img),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(85.dp),
            )

            Column(Modifier.padding(start = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    diary.emotion?.let {
                        Text(
                            text = it,
                        )
                    }

                    Box(
                        modifier = Modifier.clickable {},
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.mindrecord_horizontal),
                            contentDescription = null,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = diary.title,
                    style = AfternoteDesign.typography.bodySmallB,
                    color = Color(0xFF000000).copy(alpha = 0.8f),
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = diary.content,
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray5,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryComponentPreview() {
    AfternoteTheme {
        DiaryComponent(
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
