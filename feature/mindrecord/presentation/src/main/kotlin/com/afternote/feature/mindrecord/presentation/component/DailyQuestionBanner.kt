package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

private val BannerGradientCenter = Color(0xFFB7C4CD)
private val BannerGradientEdge = Color(0xFFECF0F3)

/**
 * Figma 2372:22574 — 데일리기록 작성 화면의 "오늘의 추천 질문" 접이식 배너.
 */
@Composable
fun DailyQuestionBanner(
    questionText: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    dayNumber: Int? = null,
) {
    // 본문 퇴장·간격 축소·화살표 회전이 같은 길이로 함께 움직이도록 값을 맞춘다 (#722).
    val transition = updateTransition(targetState = expanded, label = "bannerExpand")
    val contentSpacing by transition.animateDp(label = "spacing") { open -> if (open) 8.dp else 0.dp }
    val arrowRotation by transition.animateFloat(label = "arrow") { open -> if (open) 180f else 0f }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .drawWithCache {
                    val brush =
                        Brush.radialGradient(
                            colors = listOf(BannerGradientCenter.copy(alpha = 0.7f), BannerGradientEdge),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 2f,
                        )
                    onDrawBehind { drawRect(brush) }
                }.clickable(role = Role.Button, onClick = onToggle)
                .padding(16.dp),
        // 간격도 같은 전환에 태운다. 종전에는 본문만 AnimatedVisibility 였고 간격과 화살표
        // 회전은 즉시 바뀌어, 접기 동작이 한 흐름으로 이어지지 않고 끊겨 보였다 (#722).
        verticalArrangement = Arrangement.spacedBy(contentSpacing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(AfternoteDesign.colors.gray6),
                )
                if (dayNumber != null) {
                    Text(
                        text = stringResource(MindRecordR.string.mindrecord_daily_question_banner_day, dayNumber),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray7,
                    )
                }
                Text(
                    text = stringResource(MindRecordR.string.mindrecord_daily_question_banner_label),
                    style = AfternoteDesign.typography.mono,
                    color = AfternoteDesign.colors.gray7,
                )
            }
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_arrowdown),
                contentDescription = stringResource(MindRecordR.string.mindrecord_daily_question_banner_toggle_cd),
                tint = AfternoteDesign.colors.gray9,
                modifier =
                    Modifier
                        .size(16.dp)
                        .rotate(arrowRotation),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Text(
                    text = questionText,
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray9,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "펼침")
@Composable
private fun DailyQuestionBannerExpandedPreview() {
    AfternoteTheme {
        DailyQuestionBanner(
            questionText = "오늘 하루, 누구에게 가장 고마웠나요?",
            expanded = true,
            onToggle = {},
            dayNumber = 21,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, name = "접힘")
@Composable
private fun DailyQuestionBannerCollapsedPreview() {
    AfternoteTheme {
        DailyQuestionBanner(
            questionText = "오늘 하루, 누구에게 가장 고마웠나요?",
            expanded = false,
            onToggle = {},
            dayNumber = 21,
            modifier = Modifier.padding(20.dp),
        )
    }
}
