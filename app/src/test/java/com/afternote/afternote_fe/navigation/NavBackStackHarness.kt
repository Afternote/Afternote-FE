package com.afternote.afternote_fe.navigation

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation3.runtime.entryProvider
import com.afternote.core.ui.Route
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureNavigationCallbacks
import com.afternote.feature.home.presentation.HomeTabActions
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.onboarding.presentation.navigation.OnboardingExternalActions
import com.afternote.feature.setting.presentation.navigation.SettingExternalActions
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterNavActions

internal class NavBackStackHarness(
    val appState: AppState,
    val onboardingExternalActions: OnboardingExternalActions,
    val settingExternalActions: SettingExternalActions,
    val timeLetterActions: TimeLetterNavActions,
    val homeActions: HomeTabActions,
    val backDispatcher: OnBackPressedDispatcher,
    val timeLetterRoutes: MutableList<String>,
) {
    val navController = RootNav3TestController(appState, timeLetterRoutes)
}

internal class RootNav3TestController(
    private val appState: AppState,
    private val timeLetterRoutes: List<String>,
) {
    fun backStackRouteNames(): List<String> =
        listOf("NavHostRoot") +
            appState.backStack.map { (it as? Route)?.let { route -> route::class.simpleName ?: "Route" } ?: "${it::class.simpleName}" } +
            if (appState.activeStack == AppStack.TIMELETTER) timeLetterRoutes else emptyList()

    fun currentRouteName(): String = backStackRouteNames().last()

    fun popBackStack(): Boolean = appState.exitCurrentEntry()

    fun visibleRouteNames(): List<String> = listOf(currentRouteName())
}

@Composable
internal fun SkeletonAppNavigation(
    startDestination: Route,
    onReady: (NavBackStackHarness) -> Unit,
) {
    val appState = rememberAfternoteAppState(startDestination)
    val onboardingExternalActions = rememberOnboardingExternalActions(appState)
    val settingExternalActions = rememberSettingExternalActions(appState)
    val timeLetterRoutes =
        rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { mutableStateListOf<String>().apply { addAll(it) } })) {
            mutableStateListOf("TimeLetterHomeRoute")
        }
    val timeLetterActions =
        remember(timeLetterRoutes) {
            object : TimeLetterNavActions {
                override fun onSettingClick() {
                    appState.navigate(Route.Setting())
                }

                override fun onNavigateToWrite() {
                    timeLetterRoutes.add("TimeLetterWriteRoute")
                }

                override fun onNavigateToEdit(timeLetterId: Long) {
                    timeLetterRoutes.add("TimeLetterWriteRoute")
                }

                override fun onWriteBack() {
                    if (timeLetterRoutes.size > 1) timeLetterRoutes.removeAt(timeLetterRoutes.lastIndex)
                }

                override fun onNavigateToDraft() {
                    timeLetterRoutes.add("TimeLetterDraftRoute")
                }

                override fun onDraftBack() {
                    if (timeLetterRoutes.size > 1) timeLetterRoutes.removeAt(timeLetterRoutes.lastIndex)
                }

                override fun onNavigateToRecipient() {
                    timeLetterRoutes.add("TimeLetterRecipientRoute")
                }

                override fun onRecipientBack() {
                    if (timeLetterRoutes.size > 1) timeLetterRoutes.removeAt(timeLetterRoutes.lastIndex)
                }

                override fun onNavigateToDetail(timeLetterId: Long) {
                    timeLetterRoutes.add("TimeLetterDetailRoute")
                }

                override fun onDetailBack() {
                    if (timeLetterRoutes.size > 1) timeLetterRoutes.removeAt(timeLetterRoutes.lastIndex)
                }

                override fun onNavigateToRecipientFilter() {
                    timeLetterRoutes.add("TimeLetterRecipientFilterRoute")
                }

                override fun onRecipientFilterBack() {
                    if (timeLetterRoutes.size > 1) timeLetterRoutes.removeAt(timeLetterRoutes.lastIndex)
                }
            }
        }
    val homeActions = rememberHomeTabActions(appState, onRetryLoad = { })
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
    val rootCallbacks =
        remember(appState) {
            object : FeatureNavigationCallbacks {
                override fun exit() {
                    appState.exitCurrentEntry()
                }

                override fun onAtRootChanged(isAtRoot: Boolean) = Unit
            }
        }
    SideEffect {
        onReady(
            NavBackStackHarness(
                appState,
                onboardingExternalActions,
                settingExternalActions,
                timeLetterActions,
                homeActions,
                backDispatcher,
                timeLetterRoutes,
            ),
        )
    }
    FeatureNavDisplay(
        backStack = appState.backStack,
        boundary = rootCallbacks,
        entryProvider =
            entryProvider {
                entry<Route.Onboarding> { BasicText("Onboarding") }
                entry<Route.Home> { statefulStub("Home") }
                entry<Route.MindRecord> { BasicText("MindRecord") }
                entry<Route.TimeLetter> { BasicText("TimeLetter") }
                entry<Route.Afternote> { statefulStub("Afternote") }
                entry<Route.ReceivedAfternote> { BasicText("ReceivedAfternote") }
                entry<Route.Receiver> { BasicText("Receiver") }
                entry<Route.Setting> { BasicText("Setting") }
            },
    )
}

@Composable
private fun statefulStub(label: String) {
    var taps by rememberSaveable { mutableIntStateOf(0) }
    BasicText(text = "$label#$taps", modifier = Modifier.clickable { taps++ })
}

internal fun NavDestination.simpleRouteName(): String =
    route?.substringBefore('/')?.substringBefore('?')?.substringAfterLast('.') ?: "NavHostRoot"
