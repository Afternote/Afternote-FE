package com.afternote.feature.afternote.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteEditStateHandling
import com.afternote.feature.afternote.presentation.author.navigation.AfternoteNavGraphParams
import com.afternote.feature.afternote.presentation.author.navigation.afternoteNavGraph
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.LocalAfternoteUsesAppBottomBar

@Composable
fun AfternoteScreen(
    modifier: Modifier = Modifier,
    onBottomNavTabClick: (BottomNavTab) -> Unit = {},
) {
    val hostViewModel: AfternoteHostViewModel = hiltViewModel()
    val useFake by hostViewModel.dataProviderSwitch.useFakeState.collectAsStateWithLifecycle()
    val afternoteProvider =
        remember(useFake) { hostViewModel.dataProviderSwitch.getCurrentAfternoteEditDataProvider() }

    val navController = rememberNavController()
    val playlistHolder = remember { MemorialPlaylistStateHolder() }
    val editStateHolder = remember { mutableStateOf<AfternoteEditState?>(null) }
    val editHandling =
        remember {
            AfternoteEditStateHandling(
                holder = editStateHolder,
                onClear = { editStateHolder.value = null },
            )
        }
    val items: SnapshotStateList<Item> = remember { mutableStateListOf() }

    CompositionLocalProvider(LocalAfternoteUsesAppBottomBar provides true) {
        NavHost(
            navController = navController,
            startDestination = AfternoteRoute.AfternoteListRoute,
            modifier = modifier,
        ) {
            afternoteNavGraph(
                navController = navController,
                params =
                    AfternoteNavGraphParams(
                        afternoteItemsProvider = { items.toList() },
                        onItemsUpdated = { newItems ->
                            items.clear()
                            items.addAll(newItems)
                        },
                        playlistStateHolder = playlistHolder,
                        afternoteProvider = afternoteProvider,
                        userNameProvider = { "" },
                        editStateHandling = editHandling,
                        listRefresh = null,
                        onNavigateToSelectReceiver = {},
                    ),
                onNavTabSelected = onBottomNavTabClick,
            )
        }
    }
}
