package com.afternote.feature.mindrecord.presentation.model


sealed class DayContent {
    data class NumberOnly(val day: Int) : DayContent()        // 숫자만 (dot 없음)
    data class NumberWithDot(val day: Int) : DayContent()     // 숫자 + 하단 점
    data class EmojiWithDot(val emoji: String) : DayContent() // 이모지 + 하단 점
    data class EmojiOnly(val emoji: String) : DayContent()    // 이모지만 (dot 없음)
}

// 배경 원형 색상 타입
enum class DayBackground {
    None, Green, Pink
}

data class DayItem(
    val label: String,          // 요일 레이블
    val content: DayContent,
    val background: DayBackground = DayBackground.None
)