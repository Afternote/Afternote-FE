package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray8
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DayBackground
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.model.DayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyMoodCalendar(modifier: Modifier = Modifier) {
    // 이미지 기준 샘플 데이터
    val days =
        listOf(
            DayItem("월", DayContent.NumberOnly(10)),
            DayItem("화", DayContent.NumberWithDot(2)),
            DayItem("수", DayContent.NumberOnly(12)),
            DayItem("목", DayContent.EmojiWithDot("😊"), DayBackground.Green),
            DayItem("금", DayContent.EmojiOnly("😊"), DayBackground.Green),
            DayItem("토", DayContent.NumberWithDot(2)),
            DayItem("일", DayContent.EmojiOnly("🥹"), DayBackground.Pink),
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 상단 슬라이더
        Image(
            painter = painterResource(R.drawable.mindrecord_div),
            contentDescription = null,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 요일 + 날짜 그리드
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            days.forEach { dayItem ->
                DayCell(dayItem = dayItem)
            }
        }
    }
}

@Composable
private fun DayCell(
    dayItem: DayItem,
    modifier: Modifier = Modifier,
) {
    val bgColor =
        when (dayItem.background) {
            DayBackground.Green -> Color(0xFFCCE3D3)
            DayBackground.Pink -> Color(0xFFF5DBDB)
            DayBackground.None -> Color.Transparent
        }

    Column(
        modifier = modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 요일 텍스트
        Text(
            text = dayItem.label,
            color = Color(0xFF9E9E9E),
            style = MaterialTheme.typography.labelSmall,
        )

        // 날짜/이모지 셀
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            when (val content = dayItem.content) {
                is DayContent.NumberOnly -> {
                    Text(
                        text = content.day.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Gray9,
                    )
                }

                is DayContent.NumberWithDot -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = content.day.toString(),
                            style = MaterialTheme.typography.displayMedium,
                            color = Gray9,
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        // 하단 점
                        Box(
                            modifier =
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Gray8),
                        )
                    }
                }

                is DayContent.EmojiWithDot -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = content.emoji,
                            fontSize = 20.sp,
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier =
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Gray8),
                        )
                    }
                }

                is DayContent.EmojiOnly -> {
                    Text(
                        text = content.emoji,
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun WeeklyMoodCalendarPreview() {
    AfternoteTheme {
        WeeklyMoodCalendar()
    }
}
