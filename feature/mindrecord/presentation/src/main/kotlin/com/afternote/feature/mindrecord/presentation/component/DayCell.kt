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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
    /** 사용자가 고른 날. 선택 표시는 오늘 표시와 별개다 (#724). */
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    if (model.day == null) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    // - TODAY: 검은 원 + 흰 글자
    // - ANSWERED(작성한 날): 투명 배경 + 진한 글자 + 하단 인디케이터 (diary 는 이모지, 그 외는 점)
    // - NONE/UNANSWERED: 투명 배경 + 회색 글자, 인디케이터 없음
    // 선택은 오늘보다 우선한다 — 오늘 칸을 골랐을 때도 "고른 칸" 임이 보여야 한다 (#724).
    val bgColor =
        when {
            isSelected -> AfternoteDesign.colors.gray9
            model.state == DayState.TODAY -> AfternoteDesign.colors.gray9
            else -> Color.Transparent
        }
    val textColor =
        when {
            isSelected -> AfternoteDesign.colors.white
            model.state == DayState.TODAY -> AfternoteDesign.colors.white
            model.state == DayState.ANSWERED -> AfternoteDesign.colors.gray9
            else -> AfternoteDesign.colors.gray5
        }
    val textStyle =
        when (model.state) {
            DayState.TODAY, DayState.ANSWERED -> AfternoteDesign.typography.captionLargeB
            DayState.UNANSWERED, DayState.NONE -> AfternoteDesign.typography.captionLargeR
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .then(
                    // 날짜 셀에 클릭 콜백이 아예 없어 눌러도 아무 일이 없었다 (#724).
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    },
                ).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = model.day.toString(),
                    style = textStyle,
                    color = textColor,
                )
                if (model.state == DayState.ANSWERED) {
                    Spacer(modifier = Modifier.height(2.dp))
                    type.DayIndicator(model = model, textColor = textColor)
                }
            }
        }
    }
}
