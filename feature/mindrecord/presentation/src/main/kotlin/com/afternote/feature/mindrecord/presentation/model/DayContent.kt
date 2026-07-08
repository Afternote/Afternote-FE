package com.afternote.feature.mindrecord.presentation.model

import java.time.DayOfWeek

sealed class DayContent {
    data class NumberOnly(
        val day: Int,
    ) : DayContent() // 숫자만 (dot 없음)

    data class NumberWithDot(
        val day: Int,
    ) : DayContent() // 숫자 + 하단 점

    data class EmojiWithDot(
        val day: Int,
        val emoji: String,
    ) : DayContent() // 숫자 + 하단 이모지 (일기 기록됨)

    data class EmojiOnly(
        val day: Int,
        val emoji: String,
    ) : DayContent() // 숫자 + 하단 이모지 (일기 없음)
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
