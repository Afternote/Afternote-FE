package com.afternote.feature.timeletter.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface TimeLetterRoute {
    @Serializable
    data object TimeLetterHomeRoute : TimeLetterRoute

    @Serializable
    data object TimeLetterWriteRoute : TimeLetterRoute

    @Serializable
    data object TimeLetterDraftRoute : TimeLetterRoute

    @Serializable
    data object TimeLetterRecipientRoute : TimeLetterRoute

    @Serializable
    data class TimeLetterDetailRoute(val timeLetterId: Long) : TimeLetterRoute

    @Serializable
    data object TimeLetterRecipientFilterRoute : TimeLetterRoute
}
