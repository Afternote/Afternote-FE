package com.afternote.feature.mindrecord.presentation.component
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray9

// ───────────────────────────────────────────────
// 데이터 모델
// ───────────────────────────────────────────────

data class EmotionBubble(
    val keyword: String,
    val count: Int,
    val size: Dp,           // 원 지름
    val bgColor: Color,
    val textColor: Color = Color.White,
    val offsetX: Dp,
    val offsetY: Dp
)

// ───────────────────────────────────────────────
// 메인 카드
// ───────────────────────────────────────────────

@Composable
fun EmotionKeywordCard(
    modifier: Modifier = Modifier,
    title: String = "나의 감정 키워드",
    bubbles: List<EmotionBubble> = defaultBubbles(),
    descriptionText: String = "이번 주 박서연 님의 기록에서는\n'가족'을 위한 '감사'의 마음이 엿보입니다."
) {
    val canvasHeight = 180.dp   // 버블 영역 고정 높이

    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFFFFFF),
            ),
        border = BorderStroke(1.dp, color = Color(0xFF000000).copy(alpha = 0.05f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── 타이틀 ──
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Gray9
            )

            // ── 버블 캔버스 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight)
            ) {
                bubbles.forEach { bubble ->
                    BubbleItem(
                        bubble = bubble,
                        modifier = Modifier.offset(x = bubble.offsetX, y = bubble.offsetY)
                    )
                }
            }

            // ── 구분선 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFEEEEEE))
            )

            Spacer(modifier = Modifier.height(5.dp))
            // ── 하단 설명 텍스트 ──
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.titleSmall,
                color = Gray9
            )
        }
    }
}

// ───────────────────────────────────────────────
// 개별 버블
// ───────────────────────────────────────────────

@Composable
private fun BubbleItem(
    bubble: EmotionBubble,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(bubble.size)
            .clip(CircleShape)
            .background(bubble.bgColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = bubble.keyword,
                color = bubble.textColor,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bubble.count.toString(),
                color = bubble.textColor.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

// ───────────────────────────────────────────────
// 기본 버블 데이터 (이미지 기준 위치/크기)
// ───────────────────────────────────────────────

fun defaultBubbles() = listOf(
    // 가족 - 가장 크고 왼쪽 상단, 진한 회색
    EmotionBubble(
        keyword = "가족",
        count = 8,
        size = 100.dp,
        bgColor = Color(0xFF212121),
        offsetX = 0.dp,
        offsetY = 30.dp
    ),
    // 감사 - 두 번째 크기, 중앙 상단
    EmotionBubble(
        keyword = "감사",
        count = 8,
        size = 82.dp,
        bgColor = Color(0xFF424242),
        offsetX = 88.dp,
        offsetY = 0.dp
    ),
    // 사랑 - 세 번째 크기, 중앙 하단
    EmotionBubble(
        keyword = "사랑",
        count = 8,
        size = 68.dp,
        bgColor = Color(0xFF616161),
        offsetX = 158.dp,
        offsetY = 68.dp
    ),
    // 그리움 - 가장 작고 오른쪽
    EmotionBubble(
        keyword = "그리움",
        count = 8,
        size = 58.dp,
        bgColor = Color(0xFF9E9E9E),
        offsetX = 230.dp,
        offsetY = 42.dp
    ),
)

// ───────────────────────────────────────────────
// Preview
// ───────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 360)
@Composable
private fun EmotionKeywordCardPreview() {
    AfternoteTheme {
        EmotionKeywordCard()
    }
}