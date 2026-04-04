package com.afternote.feature.afternote.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavGraphEditContext
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavGraphHomeContext
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavGraphParams
import com.afternote.feature.afternote.presentation.author.navigation.afternoteNavGraph
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.LocalAfternoteUsesAppBottomBar

@Composable
fun AfternoteScreen(
    modifier: Modifier = Modifier,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    hostViewModel: AfternoteHostViewModel = hiltViewModel(),
) {
    val appState = rememberAfternoteAppState()

    val useFake by hostViewModel.useFakeState.collectAsStateWithLifecycle()
    val afternoteProvider =
        remember(useFake) { hostViewModel.currentAfternoteEditorDataProvider }
    val items by hostViewModel.items.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalAfternoteUsesAppBottomBar provides true) {
        NavHost(
            navController = appState.navController,
            startDestination = AfternoteRoute.AfternoteHomeRoute,
            modifier = modifier,
        ) {
            afternoteNavGraph(
                navController = appState.navController,
                params =
                    AfternoteNavGraphParams(
                        home =
                            AfternoteNavGraphHomeContext(
                                afternoteListItems = items,
                                onListItemsUpdated = hostViewModel::updateListItems,
                                homeRefresh = null,
                            ),
                        edit =
                            AfternoteNavGraphEditContext(
                                playlistStateHolder = appState.playlistHolder,
                                afternoteProvider = afternoteProvider,
                                editStateHandling = appState.editHandling,
                                onNavigateToSelectReceiver = {},
                            ),
                        userName = "",
                    ),
                onNavTabSelected = onNavTabSelected,
            )
        }
    }
}
