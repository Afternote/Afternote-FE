package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
 * Figma 노드 2249:14059 — 키워드 개수(0~4)별로 버블 layout 이 다르며, 카드 자체에서 슬롯을 결정한다
 * (ViewModel 은 keyword·count 만 넘긴다).
 *
 * - 4건: 가족(96) / 감사(72) / 사랑(56) / 그리움(64)
 * - 3건: 가족(96) / 감사(72) / 사랑(56)
 * - 2건: 가족(96) / 감사(72)
 * - 1건: 가족(96)
 * - 0건: 빈 96dp 검은 원에 "0" 만 표시 + 별도 안내 메시지(호출부 책임)
 */
@Composable
fun EmotionKeywordCard(
    keywords: List<EmotionKeyword>,
    descriptionText: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.mindrecord_emotion_card_title),
) {
    val capped = keywords.take(MAX_KEYWORDS)

    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = AfternoteDesign.colors.white),
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

            // Figma Frame 128: 320×133, 버블은 그 안에서 absolute offset.
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

            Text(
                text = descriptionText,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray9,
            )
        }
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
                .background(bubbleColor(slot.rank)),
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
                // 배경이 gray9~gray5 토큰이라 글자도 반전 토큰이라야 다크에서 뒤집힌다.
                color = AfternoteDesign.colors.white,
            )
            Text(
                text = keyword.count.toString(),
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray3,
            )
        }
    }
}

@Composable
private fun EmptyBubble() {
    val slot = EMPTY_SLOT
    Box(
        modifier =
            Modifier
                .offset(x = slot.offsetX, y = slot.offsetY)
                .size(slot.size)
                .clip(CircleShape)
                .background(bubbleColor(slot.rank)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "0",
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.white,
        )
    }
}

// ── Slot ──────────────────────────────────────────────────────────────────────

/**
 * 버블 하나의 배치. 색은 **순위만** 들고 실제 값은 컴포저블 안에서 토큰으로 푼다.
 *
 * top-level `private val` 로는 `AfternoteDesign.colors`(@Composable 게터)를 받을 수 없어
 * 종전에는 그레이 램프를 파일 상수로 다시 적었다. 값이 토큰과 4/4 일치했고 같은 파일이
 * title·border 에는 이미 토큰을 쓰고 있어 의도적 예외도 아니었다 (#634).
 */
private data class BubbleSlot(
    val size: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
    val rank: Int,
)

/** 순위(1=가장 진함)를 그레이 토큰으로 푼다. 다크에서는 토큰이 알아서 반전된다. */
@Composable
private fun bubbleColor(rank: Int): Color =
    when (rank) {
        1 -> AfternoteDesign.colors.gray9
        2 -> AfternoteDesign.colors.gray8
        3 -> AfternoteDesign.colors.gray7
        else -> AfternoteDesign.colors.gray5
    }

private const val MAX_KEYWORDS = 4
private val BUBBLE_AREA_HEIGHT = 133.dp
private val LARGE_TEXT_THRESHOLD = 72.dp

// 0건 안내용 빈 검은 원.
private val EMPTY_SLOT =
    BubbleSlot(size = 96.dp, offsetX = 112.dp, offsetY = 15.dp, rank = 1)

private fun slotsFor(count: Int): List<BubbleSlot> =
    when (count) {
        1 -> {
            listOf(
                BubbleSlot(96.dp, 112.dp, 15.dp, 1),
            )
        }

        2 -> {
            listOf(
                BubbleSlot(96.dp, 78.dp, 0.dp, 1),
                BubbleSlot(72.dp, 171.dp, 47.dp, 2),
            )
        }

        3 -> {
            listOf(
                BubbleSlot(96.dp, 48.dp, 4.dp, 1),
                BubbleSlot(72.dp, 141.dp, 51.dp, 2),
                BubbleSlot(56.dp, 205.dp, 25.dp, 3),
            )
        }

        else -> {
            // 4 이상은 4 로 cap
            listOf(
                BubbleSlot(96.dp, 37.dp, 30.dp, 1),
                BubbleSlot(72.dp, 124.dp, 0.dp, 2),
                BubbleSlot(56.dp, 149.dp, 72.dp, 3),
                BubbleSlot(64.dp, 200.dp, 36.dp, 4),
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
            descriptionText = "이번 주 박서연 님의 기록에서는 '가족'을 위한 '감사'의 마음이 엿보입니다.",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360)
@Composable
private fun EmotionKeywordCardPreview1() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords = listOf(EmotionKeyword("가족", 8)),
            descriptionText = "이번 주 박서연 님의 기록에서는 '가족'의 마음이 엿보입니다.",
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360)
@Composable
private fun EmotionKeywordCardPreview0() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords = emptyList(),
            descriptionText = "이번 주 박서연 님의 기록에서는 키워드가 나오지 않았어요.",
        )
    }
}
