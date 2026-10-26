package com.afternote.afternote_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.navigation.popUpTo
import com.afternote.core.ui.navigation.pushSingleTop

@Stable
class AppState(
    private val homeBackStack: NavBackStack<NavKey>,
    private val mindRecordBackStack: NavBackStack<NavKey>,
    private val timeLetterBackStack: NavBackStack<NavKey>,
    private val afternoteBackStack: NavBackStack<NavKey>,
    private val flowBackStack: NavBackStack<NavKey>,
    private val activeStackState: MutableState<AppStack>,
    internal val legacyMindRecordController: NavHostController? = null,
) {
    private var startDestinationInitialized = false

    var activeStack: AppStack by activeStackState
        private set

    var pendingMindRecordDestination: Any? by mutableStateOf(null)
        private set

    val backStack: NavBackStack<NavKey>
        get() =
            when (activeStack) {
                AppStack.HOME -> homeBackStack
                AppStack.RECORD -> mindRecordBackStack
                AppStack.TIMELETTER -> timeLetterBackStack
                AppStack.NOTE -> afternoteBackStack
                AppStack.FLOW -> flowBackStack
            }

    fun shouldShowBottomBar(
        isMindRecordAtRoot: Boolean,
        isTimeLetterAtRoot: Boolean,
        isAfternoteStackAtRoot: Boolean,
    ): Boolean =
        when (activeStack) {
            AppStack.HOME -> backStack.lastOrNull() == Route.Home
            AppStack.RECORD -> backStack.lastOrNull() == Route.MindRecord && isMindRecordAtRoot
            AppStack.TIMELETTER -> backStack.lastOrNull() == Route.TimeLetter && isTimeLetterAtRoot
            AppStack.NOTE -> backStack.lastOrNull() == Route.Afternote && isAfternoteStackAtRoot
            AppStack.FLOW -> false
        }

    fun getCurrentNavTab(): BottomNavTab =
        when (activeStack) {
            AppStack.HOME -> BottomNavTab.HOME
            AppStack.RECORD -> BottomNavTab.RECORD
            AppStack.TIMELETTER -> BottomNavTab.TIMELETTER
            AppStack.NOTE -> BottomNavTab.NOTE
            AppStack.FLOW -> BottomNavTab.HOME
        }

    fun navigate(route: Route) {
        when (route) {
            Route.Home, Route.MindRecord, Route.TimeLetter, Route.Afternote -> navigateToBottomBarRoute(route)
            Route.MemorySpace, Route.ReceiverMindRecord -> navigateToMindRecordDestination(route)
            else -> backStack.pushSingleTop(route)
        }
    }

    fun navigateToMindRecordDestination(destination: Any) {
        pendingMindRecordDestination = destination
        activeStack = AppStack.RECORD
        mindRecordBackStack.popUpTo(Route.MindRecord)
    }

    fun consumeMindRecordDestination(destination: Any) {
        if (pendingMindRecordDestination == destination) pendingMindRecordDestination = null
    }

    internal fun initializeLegacyStartDestination(route: Route) {
        if (startDestinationInitialized) return
        startDestinationInitialized = true
        when (route) {
            Route.Home -> {
                activeStack = AppStack.HOME
            }

            Route.MindRecord -> {
                activeStack = AppStack.RECORD
            }

            Route.TimeLetter -> {
                activeStack = AppStack.TIMELETTER
            }

            Route.Afternote -> {
                activeStack = AppStack.NOTE
            }

            else -> {
                flowBackStack.clear()
                flowBackStack.add(route)
                activeStack = AppStack.FLOW
            }
        }
    }

    fun navigateToBottomBarRoute(route: Route) {
        when (route) {
            Route.Home -> {
                activeStack = AppStack.HOME
                homeBackStack.popUpTo(Route.Home)
            }

            Route.MindRecord -> {
                activeStack = AppStack.RECORD
                mindRecordBackStack.popUpTo(Route.MindRecord)
            }

            Route.TimeLetter -> {
                activeStack = AppStack.TIMELETTER
                timeLetterBackStack.popUpTo(Route.TimeLetter)
            }

            Route.Afternote -> {
                activeStack = AppStack.NOTE
                afternoteBackStack.popUpTo(Route.Afternote)
            }

            else -> {
                backStack.pushSingleTop(route)
            }
        }
    }

    fun replaceAllWithHome() {
        homeBackStack.clear()
        homeBackStack.add(Route.Home)
        pendingMindRecordDestination = null
        activeStack = AppStack.HOME
    }

    fun replaceAllWithOnboarding() {
        flowBackStack.clear()
        flowBackStack.add(Route.Onboarding)
        homeBackStack.clear()
        homeBackStack.add(Route.Home)
        mindRecordBackStack.clear()
        mindRecordBackStack.add(Route.MindRecord)
        timeLetterBackStack.clear()
        timeLetterBackStack.add(Route.TimeLetter)
        afternoteBackStack.clear()
        afternoteBackStack.add(Route.Afternote)
        pendingMindRecordDestination = null
        activeStack = AppStack.FLOW
    }

    fun exitCurrentEntry(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        if (activeStack in setOf(AppStack.RECORD, AppStack.TIMELETTER, AppStack.NOTE)) {
            navigateToBottomBarRoute(Route.Home)
            return true
        }
        return false
    }

    companion object {
        fun initialStack(route: Route): AppStack =
            when (route) {
                Route.Home -> AppStack.HOME
                Route.MindRecord -> AppStack.RECORD
                Route.TimeLetter -> AppStack.TIMELETTER
                Route.Afternote -> AppStack.NOTE
                else -> AppStack.FLOW
            }
    }

    internal constructor(navController: NavHostController) :
        this(
            homeBackStack = NavBackStack(Route.Home),
            mindRecordBackStack = NavBackStack(Route.MindRecord),
            timeLetterBackStack = NavBackStack(Route.TimeLetter),
            afternoteBackStack = NavBackStack(Route.Afternote),
            flowBackStack = NavBackStack(Route.Home),
            activeStackState = mutableStateOf(AppStack.HOME),
            legacyMindRecordController = navController,
        )
}

enum class AppStack {
    HOME,
    RECORD,
    TIMELETTER,
    NOTE,
    FLOW,
}

@Composable
fun rememberAfternoteAppState(startDestination: Route = Route.Home): AppState {
    val homeBackStack: NavBackStack<NavKey> = rememberNavBackStack(Route.Home)
    val mindRecordBackStack: NavBackStack<NavKey> = rememberNavBackStack(Route.MindRecord)
    val timeLetterBackStack: NavBackStack<NavKey> = rememberNavBackStack(Route.TimeLetter)
    val afternoteBackStack: NavBackStack<NavKey> = rememberNavBackStack(Route.Afternote)
    val flowBackStack: NavBackStack<NavKey> = rememberNavBackStack(startDestination)
    val activeStack = rememberSaveable(startDestination) { mutableStateOf(AppState.initialStack(startDestination)) }
    return remember(homeBackStack, mindRecordBackStack, timeLetterBackStack, afternoteBackStack, flowBackStack, activeStack) {
        AppState(homeBackStack, mindRecordBackStack, timeLetterBackStack, afternoteBackStack, flowBackStack, activeStack)
    }
}
