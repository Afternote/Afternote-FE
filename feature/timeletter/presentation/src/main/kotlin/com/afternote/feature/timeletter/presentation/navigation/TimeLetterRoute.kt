package com.afternote.feature.timeletter.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface TimeLetterRoute {
    @Serializable
    data object TimeLetterHomeRoute : TimeLetterRoute

    /**
     * 편지 작성·수정 화면.
     *
     * [timeLetterId] 가 있으면 그 편지를 고친다. [prefillReceiverId] 는 새 편지의 수신인을 미리 고른다 —
     * 기념일 알림(「오늘은 ○○님 생신이에요」)이 그 수신인 앞으로 바로 쓰게 연다 (#2321).
     */
    @Serializable
    data class TimeLetterWriteRoute(
        val timeLetterId: Long? = null,
        val prefillReceiverId: Long? = null,
    ) : TimeLetterRoute

    @Serializable
    data object TimeLetterDraftRoute : TimeLetterRoute

    @Serializable
    data object TimeLetterRecipientRoute : TimeLetterRoute

    @Serializable
    data class TimeLetterDetailRoute(
        val timeLetterId: Long,
    ) : TimeLetterRoute

    @Serializable
    data object TimeLetterRecipientFilterRoute : TimeLetterRoute

    @Serializable
    data class TimeLetterRecipientDetailRoute(
        val timeLetterReceiverId: Long,
    ) : TimeLetterRoute
}
