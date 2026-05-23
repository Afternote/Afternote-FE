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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.viewmodel.WeekOption
import java.time.LocalDate

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
            4 to MindRecordCategoryUi.Diary,
            3 to MindRecordCategoryUi.DeepThought,
        ),
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel =
        weekOptions.firstOrNull { it.monday == selectedMonday }?.label
            ?: stringResource(R.string.mindrecord_weekly_report_label_fallback)

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
                    color = AfternoteDesign.colors.black.copy(alpha = 0.3f),
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
                        text = selectedLabel,
                        style = AfternoteDesign.typography.h2,
                        color = AfternoteDesign.colors.black.copy(alpha = 0.9f),
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
                    weekOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dateRange,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(33.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                counts.forEach { (count, category) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = count.toString(),
                            color = AfternoteDesign.colors.black.copy(alpha = 0.9f),
                        )
                        Text(
                            text = stringResource(category.titleRes),
                            color = AfternoteDesign.colors.black.copy(alpha = 0.4f),
                            style = AfternoteDesign.typography.captionLargeR,
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
