package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
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
 * [AfternoteHostViewModel]은 **세션 스코프 UI 초안**(플레이리스트·에디트 상태 참조)만 공유하며,
 * 작성자 목록 SSOT는 [com.afternote.feature.afternote.domain.repository.AfternoteRepository]이다.
 *
 * 네비게이션 호출은 [AfternoteNavActions]로만 전달합니다. 에디터가 포그라운드인지는
 * [NavBackStackEntry.lifecycle]이 최소 [Lifecycle.State.RESUMED]인지로 판별합니다.
 * 작성자 표시명 등 UI 데이터는 그래프 인자가 아니라 각 화면 ViewModel이 Repository로 조회한다.
 */
@Suppress("LongMethod")
fun NavGraphBuilder.afternoteNavGraph(
    /** [Route.Afternote] 그래프 엔트리 — 그래프 스코프 Host ViewModel 바인딩에 사용 */
    graphScopedParentEntry: () -> NavBackStackEntry,
    /** 루트 NavHost에서 주입하는 네비게이션 명령(화면 이동은 여기로만 캡슐화). */
    actions: AfternoteNavActions,
) {
    navigation<Route.Afternote>(startDestination = AfternoteRoute.FingerprintLoginRoute) {
        afternoteComposable<AfternoteRoute.AfternoteHomeRoute> {
            AfternoteHomeNavigation(
                onNavigateToDetail = actions::onNavigateToAfternoteDetail,
                onNavigateToGalleryDetail = actions::onNavigateToGalleryDetail,
                onNavigateToMemorialGuidelineDetail = actions::onNavigateToMemorialGuidelineDetail,
                onNavigateToNewEditor = actions::onNavigateToNewEditor,
                onNavTabSelected = actions::onBottomNavTabSelected,
            )
        }

        afternoteComposable<AfternoteRoute.DetailRoute> {
            AfternoteDetailNavigation(
                backStackEntry = it,
                onBack = actions::onPopBackStack,
                onNavigateToEditor = actions::onNavigateToEditorForEdit,
            )
        }

        afternoteComposable<AfternoteRoute.GalleryDetailRoute> { _ ->
            AfternoteGalleryDetailNavigation(
                onBack = actions::onPopBackStack,
                onNavigateToEditor = actions::onNavigateToEditorForEdit,
            )
        }

        afternoteComposable<AfternoteRoute.EditorRoute> { backStackEntry ->
            // lifecycle-runtime 2.8+의 currentStateFlow를 Compose 상태로 직접 수집하여
            // LifecycleEventObserver 등록/해제 보일러플레이트를 제거한다.
            val lifecycleState by backStackEntry.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
            val isEditorRouteCurrent = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            AfternoteEditorNavigation(
                AfternoteEditorNavigationParams(
                    backStackEntry = backStackEntry,
                    playlistStateHolder = hostViewModel.playlistHolder,
                    editState = hostViewModel.editState,
                    onEditStateChanged = hostViewModel::updateEditState,
                    onEditStateClear = hostViewModel::clearEditState,
                    onNavigateToSelectReceiver = {}, // TODO: 수신인 선택 화면 라우팅 연결
                    onBottomNavTabSelected = actions::onBottomNavTabSelected,
                    isEditorRouteCurrent = isEditorRouteCurrent,
                    onPopBackStack = actions::onPopBackStack,
                    onNavigateToMemorialPlaylist = actions::onNavigateToMemorialPlaylist,
                    onSaveSuccessNavigateHome = actions::onEditorSaveSuccessNavigateHome,
                ),
            )
        }

        afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { _ ->
            AfternoteMemorialGuidelineDetailNavigation(
                onBack = actions::onPopBackStack,
                onNavigateToEditor = actions::onNavigateToEditorForEdit,
            )
        }

        afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            MemorialPlaylistEntry(
                playlistStateHolder = hostViewModel.playlistHolder,
                actions =
                    MemorialPlaylistEntryActions(
                        onBackClick = actions::onPopBackStack,
                        onNavigateToAddSongScreen = actions::onNavigateToAddSong,
                    ),
            )
        }

        afternoteComposable<AfternoteRoute.FingerprintLoginRoute> {
            AfternoteFingerprintLoginNavigation(
                onAuthenticationSuccess = actions::onFingerprintAuthSuccess,
                onShowError = actions::onFingerprintAuthError,
            )
        }

        afternoteComposable<AfternoteRoute.AddSongRoute> {
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            val addSongViewModel: AddSongViewModel = hiltViewModel()
            AfternoteAddSongNavigation(
                onPopBackStack = actions::onPopBackStack,
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
private fun graphScopedHostViewModel(graphScopedParentEntry: () -> NavBackStackEntry): AfternoteHostViewModel {
    val parentEntry = remember { graphScopedParentEntry() }
    return hiltViewModel(parentEntry)
}
