package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.AfternoteHostViewModel
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.editor.playlist.MemorialPlaylistEntry
import com.afternote.feature.afternote.presentation.author.editor.playlist.MemorialPlaylistEntryActions
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

/**
 * Afternote 피처의 네비게이션 그래프.
 *
 * 앱 모듈의 NavHost에 직접 연결되며, [Route.Afternote]를 graph route로 사용합니다.
 * 내부 모든 화면은 graph-scoped [AfternoteHostViewModel]을 공유합니다.
 */
fun NavGraphBuilder.afternoteNavGraph(
    navController: NavController,
    onNavTabSelected: (BottomNavTab) -> Unit = {},
    userName: String = "",
    homeRefresh: AfternoteHomeRefreshParams? = null,
) {
    navigation<Route.Afternote>(startDestination = AfternoteRoute.AfternoteHomeRoute) {
        afternoteComposable<AfternoteRoute.AfternoteHomeRoute> {
            val hostViewModel = graphScopedHostViewModel(navController)
            AfternoteHomeNavigation(
                navController = navController,
                onNavTabSelected = onNavTabSelected,
                onVisibleItemsUpdated = hostViewModel::updateVisibleItems,
                homeRefresh = homeRefresh,
            )
        }

        val onAfternoteDeleted = homeRefresh?.onAfternoteDeleted ?: {}

        afternoteComposable<AfternoteRoute.DetailRoute> { backStackEntry ->
            AfternoteDetailNavigation(
                backStackEntry = backStackEntry,
                navController = navController,
                userName = userName,
                onAfternoteDeleted = onAfternoteDeleted,
            )
        }

        afternoteComposable<AfternoteRoute.GalleryDetailRoute> { backStackEntry ->
            AfternoteGalleryDetailNavigation(
                backStackEntry = backStackEntry,
                navController = navController,
                userName = userName,
                onAfternoteDeleted = onAfternoteDeleted,
            )
        }

        afternoteComposable<AfternoteRoute.EditorRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController)
            val items by hostViewModel.items.collectAsStateWithLifecycle()
            val useFake by hostViewModel.useFakeState.collectAsStateWithLifecycle()
            val afternoteProvider =
                remember(useFake) { hostViewModel.currentAfternoteEditorDataProvider }

            AfternoteEditorNavigation(
                AfternoteEditorNavigationParams(
                    backStackEntry = backStackEntry,
                    navController = navController,
                    afternoteVisibleItems = items,
                    playlistStateHolder = hostViewModel.playlistHolder,
                    afternoteProvider = afternoteProvider,
                    editStateHandling = hostViewModel.editHandling,
                    onNavigateToSelectReceiver = {},
                    onBottomNavTabSelected = onNavTabSelected,
                ),
            )
        }

        afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { backStackEntry ->
            AfternoteMemorialGuidelineDetailNavigation(
                backStackEntry = backStackEntry,
                navController = navController,
                userName = userName,
                onAfternoteDeleted = onAfternoteDeleted,
            )
        }

        afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
            val hostViewModel = graphScopedHostViewModel(navController)
            MemorialPlaylistEntry(
                playlistStateHolder = hostViewModel.playlistHolder,
                actions =
                    MemorialPlaylistEntryActions(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToAddSongScreen = {
                            navController.navigate(AfternoteRoute.AddSongRoute)
                        },
                    ),
            )
        }

        afternoteComposable<AfternoteRoute.FingerprintLoginRoute> {
            AfternoteFingerprintLoginNavigation(
                navController = navController,
                onNavTabSelected = onNavTabSelected,
            )
        }

        afternoteComposable<AfternoteRoute.AddSongRoute> {
            val hostViewModel = graphScopedHostViewModel(navController)
            val addSongViewModel: AddSongViewModel = hiltViewModel()
            AfternoteAddSongNavigation(
                navController = navController,
                playlistStateHolder = hostViewModel.playlistHolder,
                viewModel = addSongViewModel,
            )
        }
    }
}

/**
 * navigation graph scope에 묶인 [AfternoteHostViewModel]을 가져옵니다.
 * 같은 graph 내 모든 화면이 동일한 인스턴스를 공유합니다.
 */
@Composable
private fun graphScopedHostViewModel(navController: NavController): AfternoteHostViewModel {
    val currentEntry = navController.currentBackStackEntry
    val parentEntry =
        remember(currentEntry) {
            navController.getBackStackEntry<Route.Afternote>() // 해당 라우트의 엔트리 중 최신 걸 가져 옴
        }
    return hiltViewModel(parentEntry) // 뷰모델의 생명 주기를 parentEntry 백스택 엔트리에 스코핑
}
