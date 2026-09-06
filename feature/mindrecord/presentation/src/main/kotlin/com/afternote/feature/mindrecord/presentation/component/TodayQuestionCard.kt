package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_answer_cta
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_header
import com.afternote.feature.mindrecord.presentation.R.string.mindrecord_today_question_unavailable

private val TodayQuestionCardGradientStart = Color(0xFFF8F8F7)
private val TodayQuestionCardGradientEnd = Color(0xFFB7CDC0)

// 질문 자리를 항상 2줄로 잡는다 — 1줄짜리 질문이 와도 로딩 스켈레톤과 높이가 같아야 한다.
private const val QUESTION_MIN_LINES = 2
private val QuestionSkeletonLineGap = 6.dp

/**
 * @param dateText 오늘 날짜 (yyyy.MM.dd). 호출부가 실제 날짜를 넘겨야 한다 — 하드코딩 기본값 금지(#397).
 * @param questionText 오늘의 질문 본문.
 * @param isQuestionLoading 질문 조회 중이면 true — 스켈레톤을 표시한다.
 *
 * 로딩과 조회 실패를 구분한다. 둘 다 하나의 폴백 문구로 뭉뚱그리면, 시안 목업 질문이
 * 오늘의 질문처럼 보이고 답변하러 들어가면 다른 질문이 나오는 상황이 된다.
 */
@Composable
fun TodayQuestionCard(
    dateText: String,
    modifier: Modifier = Modifier,
    questionText: String? = null,
    isQuestionLoading: Boolean = false,
    onAnswerClick: () -> Unit,
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

        val questionTextStyle = AfternoteDesign.typography.h3.copy(lineHeight = 30.sp)

        // 로딩·성공·실패 세 분기가 같은 높이 제약을 공유하게 한다. 높이를 상수로 박으면
        // lineHeight 를 그대로 곱한 값과 실제 Text 측정 높이가 달라(line-height trim) 어긋나고,
        // 폰트나 lineHeight 가 바뀌면 다시 어긋난다. 높이는 질문과 동일한 스타일의 빈 Text 가
        // 결정하고(그리는 것은 없다) 각 분기는 그 안을 채우기만 한다.
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "",
                style = questionTextStyle,
                minLines = QUESTION_MIN_LINES,
            )

            when {
                isQuestionLoading -> {
                    Column(
                        modifier = Modifier.matchParentSize(),
                        verticalArrangement = Arrangement.spacedBy(QuestionSkeletonLineGap),
                    ) {
                        QuestionSkeletonLine(widthFraction = 1f, modifier = Modifier.weight(1f))
                        QuestionSkeletonLine(widthFraction = 0.6f, modifier = Modifier.weight(1f))
                    }
                }

                questionText != null -> {
                    Text(
                        text = questionText,
                        style = questionTextStyle,
                        minLines = QUESTION_MIN_LINES,
                        color = AfternoteDesign.colors.black,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(mindrecord_today_question_unavailable),
                        style = questionTextStyle,
                        minLines = QUESTION_MIN_LINES,
                        color = AfternoteDesign.colors.gray6,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AfternoteActionButton(
            text = stringResource(mindrecord_today_question_answer_cta),
            containerColor = AfternoteDesign.colors.gray9,
            onClick = onAnswerClick,
        )
    }
}

@Composable
private fun QuestionSkeletonLine(
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth(widthFraction)
                .clip(RoundedCornerShape(4.dp))
                .shimmerLoadingPlaceholder(),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "오늘의 질문 - 정상")
@Composable
private fun TodayQuestionCardPreview() {
    AfternoteTheme {
        TodayQuestionCard(
            dateText = "2026.04.10",
            questionText = "오늘 내가 배운\n가장 작은 교훈은 무엇인가요?",
            modifier = Modifier.padding(16.dp),
            onAnswerClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "오늘의 질문 - 조회 실패")
@Composable
private fun TodayQuestionCardUnavailablePreview() {
    AfternoteTheme {
        TodayQuestionCard(
            dateText = "2026.04.10",
            questionText = null,
            modifier = Modifier.padding(16.dp),
            onAnswerClick = {},
        )
    }
}
