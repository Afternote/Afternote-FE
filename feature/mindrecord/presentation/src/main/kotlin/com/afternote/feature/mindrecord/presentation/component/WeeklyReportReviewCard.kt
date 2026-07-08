package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.WeekOption
import java.time.LocalDate

/**
 * 주간 리포트 상단 "WEEKLY SUMMARY" 카드 — Figma 852:11546.
 *
 * 라디얼 그라데이션 배경 위에 주차 드롭다운·날짜 범위, 그리고
 * 그라데이션 디바이더 아래 데일리 질문/일기/깊은 생각 카운트 3열.
 */
@Composable
fun WeeklyReportReviewCard(
    modifier: Modifier = Modifier,
    selectedMonday: LocalDate? = null,
    weekOptions: List<WeekOption> = emptyList(),
    onWeekSelect: (LocalDate) -> Unit = {},
    dateRange: String = "2025.11.10. - 2025.11.16.",
    counts: List<Pair<Int, MindRecordCategoryUi>> =
        listOf(
            5 to MindRecordCategoryUi.DailyQuestion,
            6 to MindRecordCategoryUi.Diary,
            3 to MindRecordCategoryUi.DeepThought,
        ),
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = weekOptions.firstOrNull { it.monday == selectedMonday }
    val selectedLabel =
        selectedOption?.let { weekLabel(it.monday) }
            ?: stringResource(R.string.mindrecord_weekly_report_label_fallback)

    OutlinedCard(
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .drawWithCache {
                        val brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0.0f to Color(0xFFB7C4CD).copy(alpha = 0.3f),
                                        1.0f to Color(0xFFF8F8F7),
                                    ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.6f,
                            )
                        onDrawBehind { drawRect(brush) }
                    }.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "WEEKLY SUMMARY",
                        style = AfternoteDesign.typography.mono,
                        color = AfternoteDesign.colors.gray6,
                    )

                    // Box로 감싸서 DropdownMenu 앵커 잡기
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { expanded = true },
                        ) {
                            Text(
                                text = selectedLabel,
                                style = AfternoteDesign.typography.h2.copy(fontWeight = FontWeight.Normal),
                                color = AfternoteDesign.colors.gray9,
                            )
                            Icon(
                                painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_arrowdown),
                                contentDescription = null,
                                tint = AfternoteDesign.colors.black.copy(alpha = 0.3f),
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = Color.White,
                        ) {
                            weekOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = weekLabel(option.monday),
                                            style = AfternoteDesign.typography.h3,
                                            color =
                                                if (option.monday == selectedMonday) {
                                                    AfternoteDesign.colors.black.copy(alpha = 0.9f)
                                                } else {
                                                    AfternoteDesign.colors.black.copy(alpha = 0.3f)
                                                },
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        if (option.monday != selectedMonday) {
                                            onWeekSelect(option.monday)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Text(
                        text = dateRange,
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray6,
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.mindrecord_up),
                    contentDescription = null,
                    tint = AfternoteDesign.colors.black.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 양끝 투명 그라데이션 디바이더
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

                Row(modifier = Modifier.fillMaxWidth()) {
                    counts.forEach { (count, category) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = count.toString(),
                                style =
                                    AfternoteDesign.typography.inter.copy(
                                        fontWeight = FontWeight.Light,
                                        fontSize = 24.sp,
                                        lineHeight = 36.sp,
                                    ),
                                color = AfternoteDesign.colors.gray9,
                            )
                            Text(
                                text = stringResource(category.titleRes),
                                color = AfternoteDesign.colors.gray6,
                                style = AfternoteDesign.typography.footnoteCaption,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun weekLabel(monday: LocalDate): String =
    stringResource(
        R.string.mindrecord_weekly_report_label_format,
        monday.monthValue,
        (monday.dayOfMonth - 1) / 7 + 1,
    )

@Preview(showBackground = true)
@Composable
private fun WeeklyReportScreenPreview() {
    AfternoteTheme {
        WeeklyReportReviewCard()
    }
}
