package com.afternote.feature.afternote.presentation.author.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.editor.playlist.MemorialPlaylistRouteScreen
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

fun NavGraphBuilder.afternoteNavGraph(
    navController: NavController,
    params: AfternoteNavGraphParams,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
) {
    val afternoteProvider = params.edit.afternoteProvider

    afternoteComposable<AfternoteRoute.AfternoteHomeRoute> {
        AfternoteHomeDestination(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
            onItemsChanged = params.home.onItemsUpdated,
            homeRefresh = params.home.homeRefresh,
        )
    }

    val onAfternoteDeleted = params.home.homeRefresh?.onAfternoteDeleted ?: {}

    afternoteComposable<AfternoteRoute.DetailRoute> { backStackEntry ->
        AfternoteDetailDestination(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userName,
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.GalleryDetailRoute> { backStackEntry ->
        AfternoteGalleryDetailDestination(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userName,
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.EditRoute> { backStackEntry ->
        val currentItems = params.home.afternoteListItems
        AfternoteEditorDestination(
            AfternoteEditorDestinationParams(
                backStackEntry = backStackEntry,
                navController = navController,
                afternoteListItems = currentItems,
                playlistStateHolder = params.edit.playlistStateHolder,
                afternoteProvider = afternoteProvider,
                editStateHandling = params.edit.editStateHandling,
                onNavigateToSelectReceiver = params.edit.onNavigateToSelectReceiver,
                onBottomNavTabSelected = onNavTabSelected,
            ),
        )
    }

    afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { backStackEntry ->
        AfternoteMemorialGuidelineDetailDestination(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userName,
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
        MemorialPlaylistRouteScreen(
            playlistStateHolder = params.edit.playlistStateHolder,
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
            playlistStateHolder = params.edit.playlistStateHolder,
            viewModel = addSongViewModel,
        )
    }
}
