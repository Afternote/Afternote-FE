package com.afternote.feature.afternote.presentation.author.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.edit.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.edit.playlist.MemorialPlaylistRouteScreen
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

fun NavGraphBuilder.afternoteNavGraph(
    navController: NavController,
    params: AfternoteNavGraphParams,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
) {
    val afternoteProvider = params.afternoteProvider

    afternoteComposable<AfternoteRoute.AfternoteListRoute> {
        AfternoteListDestination(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
            onItemsChanged = params.onItemsUpdated,
            listRefresh = params.listRefresh,
        )
    }

    val onAfternoteDeleted = params.listRefresh?.onAfternoteDeleted ?: {}

    afternoteComposable<AfternoteRoute.DetailRoute> { backStackEntry ->
        AfternoteDetailDestination(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userNameProvider(),
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.GalleryDetailRoute> { backStackEntry ->
        AfternoteGalleryDetailDestination(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userNameProvider(),
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.EditRoute> { backStackEntry ->
        val currentItems = params.afternoteItemsProvider()
        AfternoteEditDestination(
            AfternoteEditDestinationParams(
                backStackEntry = backStackEntry,
                navController = navController,
                afternoteItems = currentItems,
                playlistStateHolder = params.playlistStateHolder,
                afternoteProvider = afternoteProvider,
                editStateHandling = params.editStateHandling,
                onNavigateToSelectReceiver = params.onNavigateToSelectReceiver,
                onBottomNavTabSelected = onNavTabSelected,
            ),
        )
    }

    afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { backStackEntry ->
        AfternoteMemorialGuidelineDetailDestination(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userNameProvider(),
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
        MemorialPlaylistRouteScreen(
            playlistStateHolder = params.playlistStateHolder,
            onBackClick = { navController.popBackStack() },
            onNavigateToAddSongScreen = { navController.navigate(AfternoteRoute.AddSongRoute) },
        )
    }

    afternoteComposable<AfternoteRoute.FingerprintLoginRoute> {
        AfternoteFingerprintLoginDestination(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
        )
    }

    afternoteComposable<AfternoteRoute.AddSongRoute> {
        val addSongViewModel: AddSongViewModel = hiltViewModel()
        AfternoteAddSongDestination(
            navController = navController,
            playlistStateHolder = params.playlistStateHolder,
            viewModel = addSongViewModel,
        )
    }
}
