package com.afternote.feature.mindrecord.presentation.model

import java.time.DayOfWeek

/**
 * 주간 캘린더 한 칸의 표현.
 *
 * 이모지와 점은 **배타적**이다 — 디자이너 정의(2026-04-06)가 "이모티콘 설정 시 이모티콘을,
 * 미설정한 것들은 점으로" 이고, 시안 캘린더에도 둘이 함께 있는 칸이 없다 (#749).
 */
sealed class DayContent {
    data class NumberOnly(
        val day: Int,
    ) : DayContent() // 숫자만 (dot 없음)

    data class NumberWithDot(
        val day: Int,
    ) : DayContent() // 숫자 + 하단 점

    data class EmojiOnly(
        val emoji: String,
    ) : DayContent() // 이모지만 (dot 없음)
}

// 배경 원형 색상 타입
enum class DayBackground {
    None,
    Green,
    Pink,
}

data class DayItem(
    val dayOfWeek: DayOfWeek, // 요일 (라벨 문자열은 UI 측에서 stringResource 로 매핑)
    val content: DayContent,
    val background: DayBackground = DayBackground.None,
)
