package com.afternote.feature.mindrecord.presentation.component.hometab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_daily_question_default_today
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_answer_cta
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_header
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(8.dp)
private val ButtonShape = RoundedCornerShape(6.dp)

/** 카드 배경 라디얼 그라데이션 — 하단 중앙의 그린 글로우가 가장자리 오프화이트로 퍼진다. */
private val GradientCenterColor = Color(0xFFB7CDC0)
private val GradientEdgeColor = Color(0xFFF8F8F7)

/** Figma drop shadow: 0 2 2.5 rgba(0,0,0,0.05) */
private val CardShadowColor = Color(0x0D000000)

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 홈 탭 전용 Daily Q&A 카드.
 * 오늘의 질문·날짜와 함께 데일리질문 답변 CTA 버튼을 보여준다.
 */
@Composable
fun HomeTabDailyQuestionCard(
    onAnswerClick: () -> Unit,
    modifier: Modifier = Modifier,
    dateText: String? = null,
    questionText: String = stringResource(mindrecord_daily_question_default_today),
) {
    val resolvedDateText =
        dateText ?: remember { LocalDate.now().format(DateFormatter) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = CardShape,
                    ambientColor = CardShadowColor,
                    spotColor = CardShadowColor,
                ).clip(CardShape)
                .drawBehind {
                    if (size.width > 0f) {
                        drawRect(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(GradientCenterColor, GradientEdgeColor),
                                    center = Offset(x = size.width / 2f, y = size.height),
                                    radius = size.width * 0.69f,
                                ),
                        )
                    }
                }.padding(30.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(mindrecord_today_question_header),
                style =
                    AfternoteDesign.typography.mono.copy(
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    ),
                color = AfternoteDesign.colors.gray6,
            )
            Text(
                text = resolvedDateText,
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = questionText,
            style =
                AfternoteDesign.typography.h3.copy(
                    lineHeight = 30.sp,
                ),
            color = AfternoteDesign.colors.gray9,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(ButtonShape)
                    .background(AfternoteDesign.colors.gray9)
                    .clickable(role = Role.Button, onClick = onAnswerClick),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(mindrecord_today_question_answer_cta),
                style = AfternoteDesign.typography.bodySmallB.copy(fontSize = 13.sp),
                color = AfternoteDesign.colors.white,
            )
            Spacer(modifier = Modifier.width(8.dp))
            RightArrowIcon(
                modifier = Modifier.size(width = 7.dp, height = 12.dp),
                tint = AfternoteDesign.colors.white,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun HomeTabDailyQuestionCardPreview() {
    AfternoteTheme {
        HomeTabDailyQuestionCard(
            modifier = Modifier.padding(16.dp),
            onAnswerClick = {},
            dateText = "2026.04.04",
        )
    }
}
