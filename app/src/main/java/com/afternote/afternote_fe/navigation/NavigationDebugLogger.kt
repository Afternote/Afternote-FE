package com.afternote.afternote_fe.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.afternote.afternote_fe.BuildConfig
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

private const val NAV_BOTTOM_BAR_DEBUG_TAG = "NavBottomBarDebug"

@Composable
fun NavigationDebugLogger(
    navEntry: NavBackStackEntry?,
    currentDestination: NavDestination?,
    showBottomBar: Boolean,
    currentTab: BottomNavTab,
) {
    if (BuildConfig.DEBUG) {
        val hierarchyRoutes =
            currentDestination?.hierarchy?.mapNotNull { it.route }?.joinToString(prefix = "[", postfix = "]")
        val hasRouteAfternoteGraph = currentDestination?.hasRoute(Route.Afternote::class) == true
        val hasRouteAfternoteHome = currentDestination?.hasRoute(AfternoteRoute.AfternoteHomeRoute::class) == true
        val hasRouteEditor = currentDestination?.hasRoute(AfternoteRoute.EditorRoute::class) == true
        LaunchedEffect(navEntry?.id, currentDestination?.route, showBottomBar, currentTab) {
            Log.d(
                NAV_BOTTOM_BAR_DEBUG_TAG,
                "entryId=${navEntry?.id} destRoute=${currentDestination?.route} " +
                    "hierarchyRoutes=$hierarchyRoutes " +
                    "shouldShowBottomBar=$showBottomBar currentNavTab=$currentTab " +
                    "hasRoute(Route.Afternote)=$hasRouteAfternoteGraph " +
                    "hasRoute(AfternoteHomeRoute)=$hasRouteAfternoteHome " +
                    "hasRoute(EditorRoute)=$hasRouteEditor",
            )
        }
    }
}
