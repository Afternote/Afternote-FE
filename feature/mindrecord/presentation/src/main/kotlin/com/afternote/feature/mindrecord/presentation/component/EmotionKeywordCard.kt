package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.EmotionKeyword

/**
 * 주간 리포트의 "나의 감정 키워드" 카드.
 *
 * Figma 노드 2249:13964(4)~2249:14023(0) — 키워드 개수(0~4)별로 버블 layout 이 다르며,
 * 카드 자체에서 슬롯(size·offset·pastel color)을 결정한다 (ViewModel 은 keyword·count 만 넘긴다).
 * 버블 아래에는 INSIGHTS 섹션(디바이더 + 본문 + 푸터)이 카드 안에 포함된다.
 *
 * - 4건: 가족(96) / 감사(72) / 사랑(56) / 그리움(64)
 * - 3건: 가족(96) / 감사(72) / 사랑(56)
 * - 2건: 가족(96) / 감사(72)
 * - 1건: 가족(96)
 * - 0건: 96dp 점선 원에 "0" 표시
 */
@Composable
fun EmotionKeywordCard(
    keywords: List<EmotionKeyword>,
    insightText: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.mindrecord_emotion_card_title),
    footerText: String = stringResource(R.string.mindrecord_insight_card_footer),
) {
    val capped = keywords.take(MAX_KEYWORDS)

    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = title,
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )

            // Figma Frame: 320×133, 버블은 그 안에서 absolute offset.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(BUBBLE_AREA_HEIGHT),
            ) {
                if (capped.isEmpty()) {
                    EmptyBubble()
                } else {
                    val slots = slotsFor(capped.size)
                    capped.forEachIndexed { index, keyword ->
                        Bubble(slot = slots[index], keyword = keyword)
                    }
                }
            }

            InsightSection(
                insightText = insightText,
                highlightKeyword = capped.firstOrNull()?.keyword,
                footerText = footerText,
            )
        }
    }
}

@Composable
private fun InsightSection(
    insightText: String,
    highlightKeyword: String?,
    footerText: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "INSIGHTS",
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AfternoteDesign.colors.gray3,
            )
        }

        // 본문 — 대표 키워드만 gray9 로 강조, 나머지는 black 70%.
        val body =
            buildAnnotatedString {
                val index = highlightKeyword?.let { insightText.indexOf(it) } ?: -1
                if (highlightKeyword != null && index >= 0) {
                    append(insightText.substring(0, index))
                    withStyle(SpanStyle(color = AfternoteDesign.colors.gray9)) {
                        append(highlightKeyword)
                    }
                    append(insightText.substring(index + highlightKeyword.length))
                } else {
                    append(insightText)
                }
            }
        Text(
            text = body,
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.black.copy(alpha = 0.7f),
        )

        Text(
            text = footerText,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
    }
}

@Composable
private fun Bubble(
    slot: BubbleSlot,
    keyword: EmotionKeyword,
) {
    Box(
        modifier =
            Modifier
                .offset(x = slot.offsetX, y = slot.offsetY)
                .size(slot.size)
                .clip(CircleShape)
                .background(slot.color),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = keyword.keyword,
                style =
                    if (slot.size >= LARGE_TEXT_THRESHOLD) {
                        AfternoteDesign.typography.bodyLargeB
                    } else {
                        AfternoteDesign.typography.bodySmallB
                    },
                color = AfternoteDesign.colors.gray9,
            )
            Text(
                text = keyword.count.toString(),
                style =
                    if (slot.size >= LARGE_COUNT_THRESHOLD) {
                        AfternoteDesign.typography.bodySmallR
                    } else {
                        AfternoteDesign.typography.footnoteCaption
                    },
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Composable
private fun EmptyBubble() {
    val borderColor = AfternoteDesign.colors.gray6
    Box(
        modifier =
            Modifier
                .offset(x = EMPTY_OFFSET_X, y = EMPTY_OFFSET_Y)
                .size(EMPTY_SIZE)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    val dash = 4.dp.toPx()
                    drawCircle(
                        color = borderColor,
                        radius = (size.minDimension - strokeWidth) / 2f,
                        style =
                            Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash), 0f),
                            ),
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "0",
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

// ── Slot ──────────────────────────────────────────────────────────────────────

private data class BubbleSlot(
    val size: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
    val color: Color,
)

private const val MAX_KEYWORDS = 4
private val BUBBLE_AREA_HEIGHT = 133.dp
private val LARGE_TEXT_THRESHOLD = 72.dp
private val LARGE_COUNT_THRESHOLD = 96.dp

// 파스텔 색상 순위: 1위(핑크) → 4위(라벤더) — Figma 2249:13964.
private val ColorRank1 = Color(0xFFFFCFCF)
private val ColorRank2 = Color(0xFFFFF7CF)
private val ColorRank3 = Color(0xFFCFEDFF)
private val ColorRank4 = Color(0xFFFFE0FB)

// 0건 안내용 점선 원 — Figma 2249:14023.
private val EMPTY_SIZE = 96.dp
private val EMPTY_OFFSET_X = 112.dp
private val EMPTY_OFFSET_Y = 15.dp

private fun slotsFor(count: Int): List<BubbleSlot> =
    when (count) {
        1 -> {
            listOf(
                BubbleSlot(96.dp, 112.dp, 15.dp, ColorRank1),
            )
        }

        2 -> {
            listOf(
                BubbleSlot(96.dp, 78.dp, 0.dp, ColorRank1),
                BubbleSlot(72.dp, 171.dp, 47.dp, ColorRank2),
            )
        }

        3 -> {
            listOf(
                BubbleSlot(96.dp, 48.dp, 4.dp, ColorRank1),
                BubbleSlot(72.dp, 141.dp, 51.dp, ColorRank2),
                BubbleSlot(56.dp, 205.dp, 25.dp, ColorRank3),
            )
        }

        else -> {
            // 4 이상은 4 로 cap
            listOf(
                BubbleSlot(96.dp, 37.dp, 30.dp, ColorRank1),
                BubbleSlot(72.dp, 124.dp, 0.dp, ColorRank2),
                BubbleSlot(56.dp, 149.dp, 72.dp, ColorRank3),
                BubbleSlot(64.dp, 200.dp, 36.dp, ColorRank4),
            )
        }
    }

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360)
@Composable
private fun EmotionKeywordCardPreview4() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords =
                listOf(
                    EmotionKeyword("가족", 8),
                    EmotionKeyword("감사", 8),
                    EmotionKeyword("사랑", 8),
                    EmotionKeyword("그리움", 8),
                ),
            insightText = "이번 주는 가족과 함께하는 시간을 가장 많이 기록하셨네요. 일상의 소중함을 느끼는 한 주였던 것 같아요.",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360)
@Composable
private fun EmotionKeywordCardPreview1() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords = listOf(EmotionKeyword("가족", 8)),
            insightText = "이번 주는 가족과 함께하는 시간을 가장 많이 기록하셨네요.",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360)
@Composable
private fun EmotionKeywordCardPreview0() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords = emptyList(),
            insightText = "이번 주 박서연 님의 기록에서는 키워드가 나오지 않았어요.",
        )
    }
}
