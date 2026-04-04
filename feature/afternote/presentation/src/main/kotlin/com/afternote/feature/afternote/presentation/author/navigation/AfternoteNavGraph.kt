package com.afternote.feature.afternote.presentation.author.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.editor.playlist.MemorialPlaylistEntry
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

fun NavGraphBuilder.afternoteNavGraph(
    navController: NavController,
    params: AfternoteNavGraphParams,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
) {
    val afternoteProvider = params.edit.afternoteProvider

    afternoteComposable<AfternoteRoute.AfternoteHomeRoute> {
        AfternoteHomeNavigation(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
            onVisibleItemsUpdated = params.home.onVisibleItemsUpdated,
            homeRefresh = params.home.homeRefresh,
        )
    }

    val onAfternoteDeleted = params.home.homeRefresh?.onAfternoteDeleted ?: {}

    afternoteComposable<AfternoteRoute.DetailRoute> { backStackEntry ->
        AfternoteDetailNavigation(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userName,
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.GalleryDetailRoute> { backStackEntry ->
        AfternoteGalleryDetailNavigation(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userName,
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.EditRoute> { backStackEntry ->
        val currentItems = params.home.afternoteVisibleItems
        AfternoteEditorNavigation(
            AfternoteEditorNavigationParams(
                backStackEntry = backStackEntry,
                navController = navController,
                afternoteVisibleItems = currentItems,
                playlistStateHolder = params.edit.playlistStateHolder,
                afternoteProvider = afternoteProvider,
                editStateHandling = params.edit.editStateHandling,
                onNavigateToSelectReceiver = params.edit.onNavigateToSelectReceiver,
                onBottomNavTabSelected = onNavTabSelected,
            ),
        )
    }

    afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { backStackEntry ->
        AfternoteMemorialGuidelineDetailNavigation(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userName,
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
        MemorialPlaylistEntry(
            playlistStateHolder = params.edit.playlistStateHolder,
            onBackClick = { navController.popBackStack() },
            onNavigateToAddSongScreen = { navController.navigate(AfternoteRoute.AddSongRoute) },
        )
    }

    afternoteComposable<AfternoteRoute.FingerprintLoginRoute> {
        AfternoteFingerprintLoginNavigation(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
        )
    }

    afternoteComposable<AfternoteRoute.AddSongRoute> {
        val addSongViewModel: AddSongViewModel = hiltViewModel()
        AfternoteAddSongNavigation(
            navController = navController,
            playlistStateHolder = params.edit.playlistStateHolder,
            viewModel = addSongViewModel,
        )
    }
}
