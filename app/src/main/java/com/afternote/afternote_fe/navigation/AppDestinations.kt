package com.afternote.afternote_fe.navigation

import com.afternote.core.ui.NavRoute

sealed class AppDestination(val route: NavRoute) {
    data object Home : AppDestination(NavRoute("home"))
    data object Mindrecord : AppDestination(NavRoute("mindrecord"))
    data object Timeletter : AppDestination(NavRoute("timeletter"))
    data object Afternote : AppDestination(NavRoute("afternote"))
}
