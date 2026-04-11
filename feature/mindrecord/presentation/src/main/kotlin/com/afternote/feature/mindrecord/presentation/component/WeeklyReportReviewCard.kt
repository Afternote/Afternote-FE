package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.title

@Composable
fun WeeklyReportReviewCard(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf("11월 2주차 리포트") }
    val reportOptions =
        listOf(
            "11월 2주차 리포트",
            "11월 1주차 리포트",
            "10월 4주차 리포트",
            "10월 3주차 리포트",
            "10월 2주차 리포트",
        )
    val report =
        listOf(
            5 to MindRecordCategory.DAILY_QUESTION,
            4 to MindRecordCategory.DIARY,
            3 to MindRecordCategory.DEEP_THOUGHT,
        )

    OutlinedCard(
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0.0f to Color(0xFFB7C4CD).copy(alpha = 0.9f),
                                        1.0f to Color(0xFFF8F8F7),
                                    ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.height * 3f,
                            )
                        onDrawBehind { drawRect(brush) }
                    }.padding(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "WEEKLY SUMMARY",
                    style = AfternoteDesign.typography.mono,
                    color = Color(0xFF000000).copy(0.3f),
                )
                Icon(
                    painter = painterResource(R.drawable.mindrecord_up),
                    contentDescription = null,
                    tint = Color(0xFF000000).copy(0.3f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ✅ Box로 감싸서 DropdownMenu 앵커 잡기
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = true },
                ) {
                    Text(
                        text = selectedReport,
                        style = AfternoteDesign.typography.h2,
                        color = Color(0xFF000000).copy(0.9f),
                    )
                    Icon(
                        painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_arrowdown),
                        contentDescription = null,
                        tint = Color(0xFF000000).copy(0.3f),
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Color.White,
                ) {
                    reportOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option,
                                    style = AfternoteDesign.typography.h3,
                                    color =
                                        if (option == selectedReport) {
                                            Color(0xFF000000).copy(0.9f)
                                        } else {
                                            Color(0xFF000000).copy(0.3f)
                                        },
                                )
                            },
                            onClick = {
                                selectedReport = option
                                expanded = false
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ✅ 날짜/통계도 Column 안에
            Text(
                text = "2025.11.10. - 2025.11.16.",
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(33.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                report.forEach { (count, category) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = count.toString(),
                            color = Color(0xFF000000).copy(0.9f),
                        )
                        Text(
                            text = category.title,
                            color = Color(0xFF000000).copy(0.4f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyReportScreenPreview() {
    AfternoteTheme {
        WeeklyReportReviewCard()
    }
}
