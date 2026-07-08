package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R as MindRecordR

/** 추천 질문 카드 라디얼 그라데이션 (Figma 2372:22138 — 중앙 rgba(183,196,205,0.7) → 가장자리 #ECF0F3). */
private val RecommendCardGradientCenter = Color(0xFFB7C4CD)
private val RecommendCardGradientEdge = Color(0xFFECF0F3)

@Composable
fun DailyQuestionWriteHeaderCard(
    modifier: Modifier = Modifier,
    questionText: String = stringResource(MindRecordR.string.mindrecord_daily_question_default_thanks),
    onAnswerClick: () -> Unit = {},
) {
    OutlinedCard(
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .height(150.dp)
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

                        onDrawBehind {
                            drawRect(brush)
                        }
                    }.padding(24.dp),
        ) {
            Text(
                text = "TODAY'S QUESTION",
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(7.5.dp))
            Text(
                text = questionText,
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(7.5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(MindRecordR.string.mindrecord_daily_question_go_answer),
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
                IconButton(
                    onClick = onAnswerClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_right),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray6,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * 데일리기록 작성 화면의 접었다 펼칠 수 있는 "오늘의 추천 질문" 카드.
 *
 * Figma 2372:22329(open) / 2372:22389(close) 의 추천 질문 카드와 1:1 매칭된다.
 *
 * @param dayCount "Day N" 카운터. 백엔드에서 아직 내려주지 않으므로 null 이면 숨긴다.
 */
@Composable
fun DailyQuestionRecommendCard(
    modifier: Modifier = Modifier,
    questionText: String = stringResource(MindRecordR.string.mindrecord_daily_question_default_thanks),
    dayCount: Int? = null,
    expanded: Boolean = true,
    onExpandToggle: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .drawWithCache {
                    val brush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0.0f to RecommendCardGradientCenter.copy(alpha = 0.7f),
                                    1.0f to RecommendCardGradientEdge,
                                ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 1.5f,
                        )

                    onDrawBehind {
                        drawRect(brush)
                    }
                }.animateContentSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dayCount != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(AfternoteDesign.colors.gray6),
                    )
                    Text(
                        text = "Day $dayCount",
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray7,
                    )
                }
                Text(
                    text = "오늘의 추천 질문",
                    style = AfternoteDesign.typography.mono,
                    color = AfternoteDesign.colors.gray7,
                )
            }
            Icon(
                painter = painterResource(R.drawable.core_ui_arrowdown),
                contentDescription = null,
                tint = AfternoteDesign.colors.gray7,
                modifier =
                    Modifier
                        .size(16.dp)
                        .rotate(if (expanded) 180f else 0f),
            )
        }
        if (expanded) {
            Text(
                text = questionText,
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteHeaderCardPreview() {
    AfternoteTheme {
        DailyQuestionWriteHeaderCard()
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionRecommendCardPreview() {
    AfternoteTheme {
        Column {
            DailyQuestionRecommendCard(dayCount = 21, expanded = true)
            Spacer(modifier = Modifier.height(12.dp))
            DailyQuestionRecommendCard(dayCount = 21, expanded = false)
        }
    }
}
