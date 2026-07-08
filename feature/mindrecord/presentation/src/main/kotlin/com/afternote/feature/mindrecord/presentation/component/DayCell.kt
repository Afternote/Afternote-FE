package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.model.DayState
import com.afternote.feature.mindrecord.presentation.model.DayUiModel
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi

@Composable
fun DayCell(
    model: DayUiModel,
    type: MindRecordCategoryUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (model.day == null) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    // Figma 2671:16704 / 2671:16718 — 캘린더 셀 상태
    // - SELECTED(선택한 날)/TODAY(레거시): 검은 원 + 흰 글자 + 그림자
    // - isToday(선택되지 않은 오늘): 연회색 원 배경
    // - ANSWERED(작성한 날): 진한 글자 + 하단 인디케이터 (diary 는 이모지, 그 외는 점)
    // - NONE/UNANSWERED: 회색 글자, 인디케이터 없음
    val isSelected = model.state == DayState.SELECTED || model.state == DayState.TODAY
    val bgColor =
        when {
            isSelected -> AfternoteDesign.colors.gray9
            model.isToday -> AfternoteDesign.colors.gray2
            else -> Color.Transparent
        }
    val textColor =
        when {
            isSelected -> AfternoteDesign.colors.white
            model.state == DayState.ANSWERED -> AfternoteDesign.colors.gray9
            else -> AfternoteDesign.colors.gray5
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (isSelected) {
                            Modifier.shadow(elevation = 4.dp, shape = CircleShape)
                        } else {
                            Modifier
                        },
                    ).clip(CircleShape)
                    .background(bgColor)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable { onClick() }
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = model.day.toString(),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = textColor,
                )
                if (model.state == DayState.ANSWERED) {
                    Spacer(modifier = Modifier.height(2.dp))
                    type.DayIndicator(model = model, textColor = AfternoteDesign.colors.gray6)
                }
            }
        }
    }
}
