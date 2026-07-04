package com.afternote.feature.timeletter.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface TimeLetterRoute {
    @Serializable
    data object TimeLetterHomeRoute : TimeLetterRoute

    @Serializable
    data class TimeLetterWriteRoute(
        val timeLetterId: Long? = null,
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
