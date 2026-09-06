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
import androidx.compose.foundation.layout.heightIn
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
    onWeekSelect: (LocalDate) -> Unit,
    dateRange: String = "2025.11.10. - 2025.11.16.",
    counts: List<Pair<Int, MindRecordCategoryUi>> =
        listOf(
            5 to MindRecordCategoryUi.DailyQuestion,
            4 to MindRecordCategoryUi.Diary,
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
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    // 높이를 **못 박지 않고 최소값으로** 둔다. 종전에는 카드가 `height(200.dp)` 고정이라
                    // 사용자가 글자 크기를 키우면 내용이 200dp 를 넘고 마지막 행(카운트 라벨)부터
                    // 잘려 나갔다 — 실측: 폰트 배율 1.3 에서 「데일리 질문」·「일기」 아랫부분이 잘린다.
                    // 저시력 사용자가 정확히 이 화면에서 값을 잃는 자리다 (#1718).
                    //
                    // 기본 배율에서는 내용이 200dp 보다 작아 시안 높이가 그대로 유지된다.
                    .heightIn(min = CardMinHeight)
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

                // 시안(node 700-35071)의 열린 메뉴는 항목 5개 높이에서 잘리고 오른쪽에 세로
                // 스크롤 표시가 있다 — 가시 항목 수보다 선택지가 많다는 표현이다. DropdownMenu
                // 의 내용은 이미 세로 스크롤이라, 높이만 묶으면 나머지가 스크롤로 넘어간다 (#729).
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = Color.White,
                    modifier = Modifier.heightIn(max = WeekMenuMaxHeight),
                ) {
                    weekOptions.forEach { option ->
                        DropdownMenuItem(
                            modifier = Modifier.height(WeekMenuItemHeight),
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

/**
 * 주차 메뉴 한 항목의 높이. Material3 `DropdownMenuItem` 의 기본 최소 높이와 같은 값을
 * 고정으로 못 박아, 가시 항목 수([WeekMenuMaxHeight])가 항목 내용에 따라 흔들리지 않게 한다.
 */
private val WeekMenuItemHeight = 48.dp

/**
 * 열린 메뉴의 최대 높이 = 항목 5개 + `DropdownMenu` 내용의 위아래 여백(각 8dp).
 *
 * 선택지가 5개를 넘으면 여기서 잘리고 나머지는 세로 스크롤로 넘어간다 (#729).
 */
private val WeekMenuMaxHeight = WeekMenuItemHeight * 5 + 16.dp

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
        WeeklyReportReviewCard(
            onWeekSelect = {},
        )
    }
}

/** 시안 높이(4327:99448 계열). 기본 배율에서는 내용이 이보다 작아 이 값이 그대로 카드 높이가 된다. */
private val CardMinHeight = 200.dp
