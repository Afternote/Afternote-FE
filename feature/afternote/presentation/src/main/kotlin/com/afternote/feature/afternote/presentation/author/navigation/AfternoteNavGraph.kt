package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.core.ui.bottombar.BottomNavTab
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
 * 실제 [androidx.navigation.NavController] 호출은 앱 루트에서만 수행하고, 이 그래프에는 이벤트만 전달합니다.
 * 작성자 표시명 등 UI 데이터는 그래프 인자가 아니라 각 화면 ViewModel이 Repository로 조회한다.
 */
@Suppress("LongMethod", "LongParameterList")
fun NavGraphBuilder.afternoteNavGraph(
    /** [Route.Afternote] 그래프 엔트리 — 그래프 스코프 Host ViewModel 바인딩에 사용 */
    graphScopedParentEntry: () -> NavBackStackEntry,
    onBottomNavTabSelected: (BottomNavTab) -> Unit = {},
    /** 현재 백스택 상단이 에디터 라우트인지(수신인 선택 결과 반영 등에 사용). 호출부에서 관찰 가능한 값으로 계산해 전달한다. */
    isEditorRouteCurrent: Boolean,
    onPopBackStack: () -> Unit,
    onNavigateToAfternoteDetail: (itemId: String) -> Unit,
    onNavigateToGalleryDetail: (itemId: String) -> Unit,
    onNavigateToMemorialGuidelineDetail: (itemId: String) -> Unit,
    onNavigateToNewEditor: (initialCategory: String?) -> Unit,
    onNavigateToEditorForEdit: (itemId: String) -> Unit,
    onNavigateToMemorialPlaylist: () -> Unit,
    onNavigateToAddSong: () -> Unit,
    onFingerprintAuthSuccess: () -> Unit,
    onFingerprintAuthError: (String) -> Unit,
    onEditorSaveSuccessNavigateHome: () -> Unit,
) {
    navigation<Route.Afternote>(startDestination = AfternoteRoute.FingerprintLoginRoute) {
        afternoteComposable<AfternoteRoute.AfternoteHomeRoute> {
            AfternoteHomeNavigation(
                onNavigateToDetail = onNavigateToAfternoteDetail,
                onNavigateToGalleryDetail = onNavigateToGalleryDetail,
                onNavigateToMemorialGuidelineDetail = onNavigateToMemorialGuidelineDetail,
                onNavigateToNewEditor = onNavigateToNewEditor,
                onNavTabSelected = onBottomNavTabSelected,
            )
        }

        afternoteComposable<AfternoteRoute.DetailRoute> {
            AfternoteDetailNavigation(
                backStackEntry = it,
                onBack = onPopBackStack,
                onNavigateToEditor = onNavigateToEditorForEdit,
            )
        }

        afternoteComposable<AfternoteRoute.GalleryDetailRoute> {
            AfternoteGalleryDetailNavigation(
                backStackEntry = it,
                onBack = onPopBackStack,
                onNavigateToEditor = onNavigateToEditorForEdit,
            )
        }

        afternoteComposable<AfternoteRoute.EditorRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            AfternoteEditorNavigation(
                AfternoteEditorNavigationParams(
                    backStackEntry = backStackEntry,
                    playlistStateHolder = hostViewModel.playlistHolder,
                    editState = hostViewModel.editState,
                    onEditStateChanged = hostViewModel::updateEditState,
                    onEditStateClear = hostViewModel::clearEditState,
                    onNavigateToSelectReceiver = {}, // TODO: 수신인 선택 화면 라우팅 연결
                    onBottomNavTabSelected = onBottomNavTabSelected,
                    isEditorRouteCurrent = isEditorRouteCurrent,
                    onPopBackStack = onPopBackStack,
                    onNavigateToMemorialPlaylist = onNavigateToMemorialPlaylist,
                    onSaveSuccessNavigateHome = onEditorSaveSuccessNavigateHome,
                ),
            )
        }

        afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> {
            AfternoteMemorialGuidelineDetailNavigation(
                backStackEntry = it,
                onBack = onPopBackStack,
                onNavigateToEditor = onNavigateToEditorForEdit,
            )
        }

        afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            MemorialPlaylistEntry(
                playlistStateHolder = hostViewModel.playlistHolder,
                actions =
                    MemorialPlaylistEntryActions(
                        onBackClick = onPopBackStack,
                        onNavigateToAddSongScreen = onNavigateToAddSong,
                    ),
            )
        }

        afternoteComposable<AfternoteRoute.FingerprintLoginRoute> {
            AfternoteFingerprintLoginNavigation(
                onAuthenticationSuccess = onFingerprintAuthSuccess,
                onShowError = onFingerprintAuthError,
            )
        }

        afternoteComposable<AfternoteRoute.AddSongRoute> {
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            val addSongViewModel: AddSongViewModel = hiltViewModel()
            AfternoteAddSongNavigation(
                onPopBackStack = onPopBackStack,
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
