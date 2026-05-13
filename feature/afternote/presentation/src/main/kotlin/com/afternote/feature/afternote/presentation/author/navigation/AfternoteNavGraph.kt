package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.afternote.presentation.AfternoteHostViewModel
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.MemorialPlaylistEntry
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.MemorialPlaylistEntryActions
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute

/**
 * Afternote 피처의 네비게이션 그래프.
 *
 * 앱 모듈의 NavHost에 직접 연결되며, [Route.Afternote]를 graph route로 사용합니다.
 * [AfternoteHostViewModel]은 그래프 스코프에서 추모 플레이리스트 곡 목록 SSOT만 보유하며,
 * Compose UI 객체(TextFieldState·SnapshotStateList·UI 파사드)는 보유하지 않습니다.
 *
 * 네비게이션 호출은 [AfternoteNavActions]로만 전달합니다. 작성자 표시명 등 UI 데이터는
 * 그래프 인자가 아니라 각 화면 ViewModel이 Repository로 조회한다.
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
            )
        }

        afternoteComposable<AfternoteRoute.DetailRoute> {
            AfternoteDetailNavigation(
                backStackEntry = it,
                onBack = actions::onPopBackStack,
                onNavigateToEditor = { itemId ->
                    actions.onNavigateToEditorForEdit(itemId, EditorCategory.SOCIAL)
                },
            )
        }

        afternoteComposable<AfternoteRoute.GalleryDetailRoute> { _ ->
            AfternoteGalleryDetailNavigation(
                onBack = actions::onPopBackStack,
                onNavigateToEditor = { itemId ->
                    actions.onNavigateToEditorForEdit(itemId, EditorCategory.GALLERY)
                },
            )
        }

        afternoteComposable<AfternoteRoute.EditorRoute> { backStackEntry ->
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            val graphSongs by hostViewModel.playlistSongs.collectAsStateWithLifecycle()
            AfternoteEditorNavigation(
                AfternoteEditorNavigationParams(
                    backStackEntry = backStackEntry,
                    graphSongs = graphSongs,
                    onReplaceSongs = hostViewModel::replaceSongs,
                    onClearSongs = hostViewModel::clearAllSongs,
                    onNavigateToSelectReceiver = {}, // TODO: 수신인 선택 화면 라우팅 연결
                    onBottomNavTabSelected = actions::onBottomNavTabSelected,
                    onPopBackStack = actions::onPopBackStack,
                    onNavigateToMemorialPlaylist = actions::onNavigateToMemorialPlaylist,
                    onSaveSuccessNavigateHome = actions::onEditorSaveSuccessNavigateHome,
                ),
            )
        }

        afternoteComposable<AfternoteRoute.MemorialGuidelineDetailRoute> { _ ->
            AfternoteMemorialGuidelineDetailNavigation(
                onBack = actions::onPopBackStack,
                onNavigateToEditor = { itemId ->
                    actions.onNavigateToEditorForEdit(itemId, EditorCategory.MEMORIAL)
                },
            )
        }

        afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> {
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            val graphSongs by hostViewModel.playlistSongs.collectAsStateWithLifecycle()
            MemorialPlaylistEntry(
                songs = graphSongs,
                actions =
                    MemorialPlaylistEntryActions(
                        onBackClick = actions::onPopBackStack,
                        onNavigateToAddSongScreen = actions::onNavigateToAddSong,
                        onClearAllSongs = hostViewModel::clearAllSongs,
                        onRemoveSongs = hostViewModel::removeSongs,
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
                onSongsAdded = hostViewModel::addSongs,
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
