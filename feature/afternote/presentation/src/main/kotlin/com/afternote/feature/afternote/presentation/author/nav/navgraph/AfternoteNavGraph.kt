package com.afternote.feature.afternote.presentation.author.nav.navgraph

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.edit.ui.MemorialPlaylistRouteScreen
import com.afternote.feature.afternote.presentation.author.edit.ui.addsong.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.nav.model.AfternoteRoute

fun NavGraphBuilder.afternoteNavGraph(
    navController: NavController,
    params: AfternoteNavGraphParams,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
) {
    val afternoteProvider = params.afternoteProvider

    afternoteComposable<AfternoteRoute.AfternoteListRoute> {
        AfternoteListRouteContent(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
            onItemsUpdated = params.onItemsUpdated,
            editStateHandling = params.editStateHandling,
            playlistStateHolder = params.playlistStateHolder,
            listRefresh = params.listRefresh,
        )
    }

    val onAfternoteDeleted = params.listRefresh?.onAfternoteDeleted ?: {}

    afternoteComposable<AfternoteRoute.DetailRoute> { backStackEntry ->
        AfternoteDetailRouteContent(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userNameProvider(),
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.GalleryDetailRoute> { backStackEntry ->
        AfternoteGalleryDetailRouteContent(
            backStackEntry = backStackEntry,
            navController = navController,
            userName = params.userNameProvider(),
            onAfternoteDeleted = onAfternoteDeleted,
        )
    }

    afternoteComposable<AfternoteRoute.EditRoute> { backStackEntry ->
        val currentItems = params.afternoteItemsProvider()
        AfternoteEditRouteContent(
            AfternoteEditRouteContentParams(
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
        AfternoteMemorialGuidelineDetailContent(
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
        AfternoteFingerprintLoginContent(
            navController = navController,
            onNavTabSelected = onNavTabSelected,
        )
    }

    afternoteComposable<AfternoteRoute.AddSongRoute> {
        val addSongViewModel: AddSongViewModel = hiltViewModel()
        AfternoteAddSongRouteContent(
            navController = navController,
            playlistStateHolder = params.playlistStateHolder,
            viewModel = addSongViewModel,
        )
    }
}
