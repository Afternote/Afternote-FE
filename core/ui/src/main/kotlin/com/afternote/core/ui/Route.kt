package com.afternote.core.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object MindRecord : Route

    @Serializable
    data object TimeLetter : Route

    @Serializable
    data object Afternote : Route
}
