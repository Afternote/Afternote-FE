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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.shimmerLoadingPlaceholder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
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
 *
 * **"0" 은 분석이 정상 종료됐을 때만 찍는다.** `emotions` 는 분석 성공분만 담기므로 빈
 * 목록 하나로는 "실제로 0건" 과 "아직 분석 중" 과 "분석 실패" 가 구분되지 않는다.
 * 대기·실패 상태에서 0 을 찍으면 키워드가 없다고 확정해 버린다 (#725).
 */
@Composable
fun EmotionKeywordCard(
    keywords: List<EmotionKeyword>,
    descriptionText: String,
    analysisStatus: EmotionAnalysisStatus,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.mindrecord_emotion_card_title),
    onRetry: (() -> Unit)? = null,
) {
    val capped = keywords.take(MAX_KEYWORDS)
    val confirmsEmptyCount =
        analysisStatus == EmotionAnalysisStatus.COMPLETED ||
            analysisStatus == EmotionAnalysisStatus.NOTHING_TO_ANALYZE

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

            // Figma Frame 128: 320×133, 버블은 그 안에서 absolute offset.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(BUBBLE_AREA_HEIGHT),
            ) {
                if (capped.isEmpty()) {
                    when {
                        // 분석이 끝났고 실제로 0건 — "0" 을 찍어 확정한다.
                        confirmsEmptyCount -> EmptyBubble()

                        // 실패는 대기와 같은 그림이면 안 된다. 종전에는 둘 다 PendingBubble 로
                        // 떨어져 시각적으로 구분되지 않았고 TalkBack 은 아무것도 읽지 않았다.
                        analysisStatus == EmotionAnalysisStatus.FAILED -> FailedBubble()

                        analysisStatus == EmotionAnalysisStatus.UNKNOWN -> UnknownBubble()

                        else -> PendingBubble()
                    }
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

            if (analysisStatus == EmotionAnalysisStatus.FAILED && onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.mindrecord_emotion_card_retry),
                        style = AfternoteDesign.typography.bodySmallB,
                        color = AfternoteDesign.colors.gray9,
                    )
                }
            }
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
                color = Color.White,
            )
            Text(
                text = keyword.count.toString(),
                style = AfternoteDesign.typography.footnoteCaption,
                color = AfternoteDesign.colors.gray3,
            )
        }
    }
}

/**
 * 분석이 끝나지 않았을 때의 자리 표시. 아직 값이 오는 중이라는 것을 shimmer 로 알리고
 * **"0" 을 찍지 않는다** — 그건 키워드가 없다고 확정하는 표시다 (#725).
 */
@Composable
private fun PendingBubble() {
    PlaceholderBubble(
        contentDescription = stringResource(R.string.mindrecord_emotion_card_pending_bubble_label),
        isLoading = true,
    )
}

/** 분석이 실패했을 때의 자리 표시. 대기와 같은 그림이면 두 상태가 구분되지 않는다. */
@Composable
private fun FailedBubble() {
    PlaceholderBubble(
        contentDescription = stringResource(R.string.mindrecord_emotion_card_failed_bubble_label),
        isLoading = false,
    )
}

/** 서버가 진행 상태를 주지 않았을 때. 실패도 대기도 아니므로 그 둘과 문구를 달리한다. */
@Composable
private fun UnknownBubble() {
    PlaceholderBubble(
        contentDescription = stringResource(R.string.mindrecord_emotion_card_unknown_bubble_label),
        isLoading = false,
    )
}

/** 분석이 끝났고 실제로 키워드가 0건. 이때만 "0" 을 찍어 확정한다. */
@Composable
private fun EmptyBubble() {
    val slot = EMPTY_SLOT
    Box(
        modifier =
            Modifier
                .offset(x = slot.offsetX, y = slot.offsetY)
                .size(slot.size)
                .clip(CircleShape)
                .background(slot.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "0",
            style = AfternoteDesign.typography.bodySmallR,
            color = Color.White,
        )
    }
}

/**
 * 값이 아직/영영 없을 때의 빈 버블. 빈 `Box` 만 두면 TalkBack 이 아무것도 읽지 않아
 * 화면에 무슨 일이 있는지 알 수 없다.
 */
@Composable
private fun PlaceholderBubble(
    contentDescription: String,
    isLoading: Boolean,
) {
    val slot = EMPTY_SLOT
    Box(
        modifier =
            Modifier
                .offset(x = slot.offsetX, y = slot.offsetY)
                .size(slot.size)
                .clip(CircleShape)
                .background(slot.color.copy(alpha = 0.3f))
                .then(if (isLoading) Modifier.shimmerLoadingPlaceholder() else Modifier)
                .semantics { this.contentDescription = contentDescription },
    )
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

// 색상 순위: 1위(가장 진함) → 4위(가장 옅음)
private val ColorRank1 = Color(0xFF212121)
private val ColorRank2 = Color(0xFF424242)
private val ColorRank3 = Color(0xFF616161)
private val ColorRank4 = Color(0xFF9E9E9E)

// 0건 안내용 빈 검은 원.
private val EMPTY_SLOT =
    BubbleSlot(size = 96.dp, offsetX = 112.dp, offsetY = 15.dp, color = ColorRank1)

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
            descriptionText = "이번 주 박서연 님의 기록에서는 '가족'을 위한 '감사'의 마음이 엿보입니다.",
            analysisStatus = EmotionAnalysisStatus.COMPLETED,
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
            analysisStatus = EmotionAnalysisStatus.COMPLETED,
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
            analysisStatus = EmotionAnalysisStatus.COMPLETED,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360, name = "분석 대기")
@Composable
private fun EmotionKeywordCardPreviewPending() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords = emptyList(),
            descriptionText = "기록을 분석하고 있어요. 잠시 뒤 이 자리에 키워드가 채워집니다.",
            analysisStatus = EmotionAnalysisStatus.PENDING,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360, name = "분석 실패")
@Composable
private fun EmotionKeywordCardPreviewFailed() {
    AfternoteTheme {
        EmotionKeywordCard(
            keywords = emptyList(),
            descriptionText = "감정 분석에 실패했어요. 다시 시도해 주세요.",
            analysisStatus = EmotionAnalysisStatus.FAILED,
            onRetry = {},
        )
    }
}
