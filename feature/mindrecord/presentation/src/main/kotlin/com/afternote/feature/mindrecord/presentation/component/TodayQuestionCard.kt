package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.button.AfternoteActionButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_daily_question_default_today
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_answer_cta
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_header

private val TodayQuestionCardGradientStart = Color(0xFFF8F8F7)
private val TodayQuestionCardGradientEnd = Color(0xFFB7CDC0)

/**
 * @param dateText 오늘 날짜 (yyyy.MM.dd). 호출부가 실제 날짜를 넘겨야 한다 — 하드코딩 기본값 금지(#397).
 * @param questionText 오늘의 질문 본문. 조회 실패 시에만 기본 문구를 사용한다.
 */
@Composable
fun TodayQuestionCard(
    dateText: String,
    modifier: Modifier = Modifier,
    questionText: String = stringResource(mindrecord_daily_question_default_today),
    onAnswerClick: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                // Figma 2372:52503 — 하단 중앙에서 퍼지는 radial gradient (#B7CDC0 → #F8F8F7)
                .drawWithCache {
                    val brush =
                        Brush.radialGradient(
                            colors = listOf(TodayQuestionCardGradientEnd, TodayQuestionCardGradientStart),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.width,
                        )
                    onDrawBehind { drawRect(brush) }
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
                        lineHeight = 15.sp,
                        fontSize = 10.sp,
                    ),
                color = AfternoteDesign.colors.gray6,
            )
            Text(
                text = dateText,
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = questionText,
            style =
                AfternoteDesign.typography.h3.copy(
                    lineHeight = 30.sp,
                ),
            color = AfternoteDesign.colors.black,
        )

        Spacer(modifier = Modifier.height(18.dp))

        AfternoteActionButton(
            text = stringResource(mindrecord_today_question_answer_cta),
            containerColor = AfternoteDesign.colors.gray9,
            onClick = onAnswerClick,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TodayQuestionCardPreview() {
    AfternoteTheme {
        TodayQuestionCard(
            dateText = "2026.04.10",
            modifier = Modifier.padding(16.dp),
        )
    }
}
