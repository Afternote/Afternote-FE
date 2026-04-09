package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.afternote.core.ui.Route
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
@Suppress("LongMethod")
fun NavGraphBuilder.afternoteNavGraph(params: AfternoteNavGraphParams) {
    val navController = params.navController
    navigation<Route.Afternote>(startDestination = AfternoteRoute.AfternoteHomeRoute) {
        afternoteComposable<AfternoteRoute.AfternoteHomeRoute> { backStackEntry ->
            // 이 목적지가 생성될 때 발급된 교유한 엔트리. 스택에 머무는 동안 변하지 않음
            // Composable Destination: 화면을 정의 하고 의존성을 주입하는 컴포저블 함수
            // 네비게이트할 때마다 엔트리가 추가되면서 블록을 실행
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
            AfternoteHomeNavigation(
                navController = navController,
                onNavTabSelected = params.onNavTabSelected,
                onVisibleItemsUpdated = hostViewModel::updateVisibleItems,
                homeRefreshEvents = hostViewModel.homeRefreshEvents,
            )
        }

        afternoteComposable<AfternoteRoute.DetailRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
            AfternoteDetailNavigation(
                backStackEntry = backStackEntry,
                navController = navController,
                userName = params.userName,
                onAfternoteDeleted = hostViewModel::requestHomeRefresh,
            )
        }

        afternoteComposable<AfternoteRoute.GalleryDetailRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
            AfternoteGalleryDetailNavigation(
                backStackEntry = backStackEntry,
                navController = navController,
                userName = params.userName,
                onAfternoteDeleted = hostViewModel::requestHomeRefresh,
            )
        }

        afternoteComposable<AfternoteRoute.EditorRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
            val items by hostViewModel.items.collectAsStateWithLifecycle()

            AfternoteEditorNavigation(
                AfternoteEditorNavigationParams(
                    backStackEntry = backStackEntry,
                    navController = navController,
                    afternoteVisibleItems = items,
                    playlistStateHolder = hostViewModel.playlistHolder,
                    editState = hostViewModel.editState,
                    onEditStateChanged = hostViewModel::updateEditState,
                    onEditStateClear = hostViewModel::clearEditState,
                    onRequestHomeRefresh = hostViewModel::requestHomeRefresh,
                    onNavigateToSelectReceiver = {},
                    onBottomNavTabSelected = params.onNavTabSelected,
                ),
            )
        }

        afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
            AfternoteMemorialGuidelineDetailNavigation(
                backStackEntry = backStackEntry,
                navController = navController,
                userName = params.userName,
                onAfternoteDeleted = hostViewModel::requestHomeRefresh,
            )
        }

        afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
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
            )
        }

        afternoteComposable<AfternoteRoute.AddSongRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(navController, backStackEntry)
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
private fun graphScopedHostViewModel(
    navController: NavController,
    // 컴포저블 데스티네이션의 엔트리 스냅샷
    // 이렇게 하지 않고 현재 엔트리를 직접 가져오면 다른 탭 라우트로 이동했을 때 리멤버 블록을 재실행하여 팝된 라우트를 찾으려고 해서 크래시
    childEntry: NavBackStackEntry,
): AfternoteHostViewModel {
    val parentEntry =
        remember(childEntry) {
            navController.getBackStackEntry<Route.Afternote>() // 해당 라우트의 그래프 엔트리 가져 옴
        }
    return hiltViewModel(parentEntry) // 뷰모델의 생명 주기를 parentEntry 백스택 엔트리에 스코핑
    // hiltViewModel의 제네릭 타입 추론을 통해 함수의 리턴 시그니처에 맞는 타입으로 반환
    // 리컴포지션 시 parentEntry에 스코핑된 AfternoteHostViewModel가 있다면 재사용, 그렇지 않다면 새로 생성
}
