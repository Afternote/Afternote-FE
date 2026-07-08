package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.DayContent
import com.afternote.feature.mindrecord.presentation.model.DayItem
import java.time.DayOfWeek

/**
 * 주간 리포트의 요일별 기록 캘린더 — Figma 852:11580.
 *
 * 상단 그라데이션 디바이더(가운데 4dp 핸들) + 요일/날짜 7열.
 * 날짜 셀: 미기록(gray5 숫자) / 일기 기록(gray9 숫자 + 점) / 감정 기록(gray9 숫자 + 이모지).
 */
@Composable
fun WeeklyMoodCalendar(
    modifier: Modifier = Modifier,
    days: List<DayItem> = defaultPreviewDays(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 상단 디바이더 — 양끝 투명 그라데이션 라인 + 가운데 4dp 핸들.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    AfternoteDesign.colors.black.copy(alpha = 0.1f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
            )
            Box(
                modifier =
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(AfternoteDesign.colors.black.copy(alpha = 0.2f)),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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
    Column(
        modifier = modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 요일 텍스트 — 10sp gray8.
        Text(
            text = stringResource(dayOfWeekLabelRes(dayItem.dayOfWeek)),
            color = AfternoteDesign.colors.gray8,
            style = AfternoteDesign.typography.footnoteCaption,
        )

        // 날짜 셀 — 40dp 원형 영역 (배경 없음).
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val content = dayItem.content) {
                is DayContent.NumberOnly -> {
                    Text(
                        text = content.day.toString(),
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.gray5,
                    )
                }

                is DayContent.NumberWithDot -> {
                    NumberWithBadge(day = content.day) {
                        Box(
                            modifier =
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(AfternoteDesign.colors.gray8),
                        )
                    }
                }

                is DayContent.EmojiWithDot -> {
                    NumberWithBadge(day = content.day) {
                        Text(
                            text = content.emoji,
                            style = AfternoteDesign.typography.footnoteCaption,
                        )
                    }
                }

                is DayContent.EmojiOnly -> {
                    NumberWithBadge(day = content.day) {
                        Text(
                            text = content.emoji,
                            style = AfternoteDesign.typography.footnoteCaption,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberWithBadge(
    day: Int,
    badge: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = day.toString(),
            style = AfternoteDesign.typography.captionLargeB,
            color = AfternoteDesign.colors.gray9,
        )
        badge()
    }
}

@androidx.annotation.StringRes
private fun dayOfWeekLabelRes(dayOfWeek: DayOfWeek): Int =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> R.string.mindrecord_calendar_day_label_mon
        DayOfWeek.TUESDAY -> R.string.mindrecord_calendar_day_label_tue
        DayOfWeek.WEDNESDAY -> R.string.mindrecord_calendar_day_label_wed
        DayOfWeek.THURSDAY -> R.string.mindrecord_calendar_day_label_thu
        DayOfWeek.FRIDAY -> R.string.mindrecord_calendar_day_label_fri
        DayOfWeek.SATURDAY -> R.string.mindrecord_calendar_day_label_sat
        DayOfWeek.SUNDAY -> R.string.mindrecord_calendar_day_label_sun
    }

private fun defaultPreviewDays(): List<DayItem> =
    listOf(
        DayItem(DayOfWeek.MONDAY, DayContent.NumberOnly(10)),
        DayItem(DayOfWeek.TUESDAY, DayContent.NumberWithDot(11)),
        DayItem(DayOfWeek.WEDNESDAY, DayContent.NumberOnly(12)),
        DayItem(DayOfWeek.THURSDAY, DayContent.EmojiWithDot(13, "😢")),
        DayItem(DayOfWeek.FRIDAY, DayContent.EmojiWithDot(14, "😢")),
        DayItem(DayOfWeek.SATURDAY, DayContent.NumberWithDot(15)),
        DayItem(DayOfWeek.SUNDAY, DayContent.EmojiOnly(16, "😢")),
    )

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun WeeklyMoodCalendarPreview() {
    AfternoteTheme {
        WeeklyMoodCalendar()
    }
}
