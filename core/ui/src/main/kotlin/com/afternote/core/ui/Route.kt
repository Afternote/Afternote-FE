package com.afternote.core.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Home : Route

    /** 홈 탭 MEMORIES 영역에서 진입하는 기억 공간(격자·카드 탐색 UI). */
    @Serializable
    data object MemorySpace : Route

    @Serializable
    data object MindRecord : Route

    @Serializable
    data object TimeLetter : Route

    @Serializable
    data object Afternote : Route
}
