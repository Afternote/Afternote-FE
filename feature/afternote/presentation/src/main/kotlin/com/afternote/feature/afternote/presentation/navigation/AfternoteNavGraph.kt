package com.afternote.feature.afternote.presentation.navigation

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
import com.afternote.feature.afternote.presentation.detail.AfternoteDetailNavigation
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorNavigation
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.editor.memorial.AddSongViewModel
import com.afternote.feature.afternote.presentation.editor.memorial.AfternoteAddSongNavigation
import com.afternote.feature.afternote.presentation.editor.memorial.MemorialPlaylistEntry
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteSelectReceiverNavigation
import com.afternote.feature.afternote.presentation.home.AfternoteHomeNavigation
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.shared.fingerprint.AfternoteFingerprintLoginNavigation

/**
 * Afternote 피처의 네비게이션 그래프.
 *
 * 앱 모듈의 NavHost에 직접 연결되며, [Route.Afternote]를 graph route로 사용합니다.
 * 에디터·추억 플레이리스트·곡 추가 화면은 [AfternoteRoute.EditorFlowRoute] 중첩 그래프에 묶여
 * 같은 [AfternoteEditorViewModel]의 폼을 공유합니다. 수신자 선택 화면(#540)은 같은 중첩 그래프에
 * 있지만 전용 ViewModel 을 쓰고, 결과는 에디터 엔트리의 SavedStateHandle 로만 돌려줍니다.
 *
 * 네비게이션 호출은 [AfternoteNavActions]로만 전달합니다. 작성자 표시명 등 UI 데이터는
 * 그래프 인자가 아니라 각 화면 ViewModel이 Repository로 조회한다.
 */
@Suppress("LongMethod")
fun NavGraphBuilder.afternoteNavGraph(
    /** [Route.Afternote] 그래프 엔트리 — 그래프 스코프 Host ViewModel 바인딩에 사용 */
    graphScopedParentEntry: () -> NavBackStackEntry,
    /** [AfternoteRoute.EditorFlowRoute] 엔트리 — 에디터 하위 세 화면이 동일한 폼 ViewModel을 공유한다. */
    editorFlowParentEntry: () -> NavBackStackEntry,
    /** 루트 NavHost에서 주입하는 네비게이션 명령(화면 이동은 여기로만 캡슐화). */
    actions: AfternoteNavActions,
) {
    navigation<Route.Afternote>(startDestination = AfternoteRoute.FingerprintLoginRoute) {
        afternoteComposable<AfternoteRoute.AfternoteHomeRoute> {
            AfternoteHomeNavigation(
                onNavigateToDetail = actions::navigateToAfternoteDetail,
                onNavigateToNewEditor = actions::navigateToNewEditor,
                onNavigateToSetting = actions::navigateToSetting,
            )
        }

        afternoteComposable<AfternoteRoute.DetailRoute> {
            AfternoteDetailNavigation(
                onNavigateBack = actions::popBack,
                onNavigateToEditor = actions::navigateToEditorForEdit,
            )
        }

        navigation<AfternoteRoute.EditorFlowRoute>(
            startDestination = AfternoteRoute.EditorRoute,
        ) {
            afternoteComposable<AfternoteRoute.EditorRoute> { backStackEntry ->
                val editorViewModel = backStackEntry.editorFlowViewModel(editorFlowParentEntry)
                AfternoteEditorNavigation(
                    backStackEntry = backStackEntry,
                    editViewModel = editorViewModel,
                    onNavigateToMemorialPlaylist = actions::navigateToMemorialPlaylist,
                    onNavigateToSelectReceiver = actions::navigateToSelectReceiver,
                    onPopBackStack = actions::popBack,
                    onSaveSuccessNavigateHome = actions::popToAfternoteHome,
                )
            }

            afternoteComposable<AfternoteRoute.SelectReceiverRoute> { backStackEntry ->
                // 폼에 이미 있는 수신자를 선택 상태로 열어 준다 — 선택 화면은 «추가분» 이 아니라
                // 확정된 수신자 전체를 돌려주므로, 여는 시점의 폼이 곧 초기 선택이다 (#1426).
                val editorViewModel = backStackEntry.editorFlowViewModel(editorFlowParentEntry)
                val editorUiState by editorViewModel.uiState.collectAsStateWithLifecycle()
                AfternoteSelectReceiverNavigation(
                    preselectedReceiverIds = editorUiState.form.afternoteEditReceivers.map { it.id },
                    onPopBackStack = actions::popBack,
                    onReceiversConfirmed = actions::popBackWithSelectedReceivers,
                )
            }

            afternoteComposable<AfternoteRoute.MemorialPlaylistRoute> { backStackEntry ->
                val editorViewModel = backStackEntry.editorFlowViewModel(editorFlowParentEntry)
                val editorUiState by editorViewModel.uiState.collectAsStateWithLifecycle()
                MemorialPlaylistEntry(
                    songs = editorUiState.form.memorialPlaylistSongs,
                    onBackClick = actions::popBack,
                    onNavigateToAddSongScreen = actions::navigateToAddSong,
                    onClearAllSongs = editorViewModel::clearMemorialPlaylistSongs,
                    onRemoveSongs = editorViewModel::removeMemorialPlaylistSongs,
                )
            }

            afternoteComposable<AfternoteRoute.AddSongRoute> { backStackEntry ->
                val editorViewModel = backStackEntry.editorFlowViewModel(editorFlowParentEntry)
                val addSongViewModel: AddSongViewModel = hiltViewModel()
                AfternoteAddSongNavigation(
                    onPopBackStack = actions::popBack,
                    onSongsAdded = editorViewModel::addMemorialPlaylistSongs,
                    viewModel = addSongViewModel,
                )
            }
        }

        afternoteComposable<AfternoteRoute.FingerprintLoginRoute> {
            val hostViewModel = graphScopedHostViewModel(graphScopedParentEntry)
            val isPasskeyRegistered by hostViewModel.isPasskeyRegistered.collectAsStateWithLifecycle()
            AfternoteFingerprintLoginNavigation(
                isPasskeyRegistered = isPasskeyRegistered,
                onAuthenticationSuccess = actions::replaceFingerprintLoginWithAfternoteHome,
                onShowError = actions::onFingerprintAuthFailed,
            )
        }
    }
}

/** 에디터 하위 destination이 [AfternoteRoute.EditorFlowRoute] 범위의 폼 ViewModel을 공유하도록 한다. */
@Composable
private fun NavBackStackEntry.editorFlowViewModel(editorFlowParentEntry: () -> NavBackStackEntry): AfternoteEditorViewModel {
    val parentEntry = remember(this) { editorFlowParentEntry() }
    return hiltViewModel(parentEntry)
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
