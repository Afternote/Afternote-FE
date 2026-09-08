package com.afternote.feature.afternote.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorNavigation
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel
import com.afternote.feature.afternote.presentation.editor.memorial.AddSongViewModel
import com.afternote.feature.afternote.presentation.editor.memorial.AfternoteAddSongNavigation
import com.afternote.feature.afternote.presentation.editor.memorial.MemorialPlaylistEntry
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteSelectReceiverNavigation
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute

/**
 * 에디터 흐름(작성·수신자 선택·추억 플레이리스트·곡 추가)의 **흐름 전용 로컬 스택**.
 *
 * Nav2 에서는 `navigation<EditorFlowRoute>` 중첩 그래프가 네 화면의 공용 `ViewModelStore` 를
 * 갖고 있었다. Nav3 엔 중첩 그래프가 없으므로 흐름 키를 바깥 스택의 entry 로 두고 그 안에서
 * 다시 스택을 연다 — [AfternoteEditorViewModel] 은 이 entry 범위라 네 화면이 같은 폼을
 * 공유하고, 흐름을 벗어나면 정리된다. 이관 전과 같은 수명이다.
 *
 * 수신자 선택 결과는 Nav2 의 «이전 엔트리 SavedStateHandle» 대신 이 공유 ViewModel 이 나른다
 * ([AfternoteEditorViewModel.onReceiversSelected]) — 저장 위치가 같은 `SavedStateHandle` 이라
 * 프로세스 재생성 성질도 그대로다.
 *
 * @param key 흐름 진입 키(`itemId`·`initialType`). 흐름 ViewModel 에 assisted 로 주입된다.
 * @param onExitFlow 흐름 스택 바닥에서의 back — 바깥 스택이 이 흐름 entry 를 내린다.
 * @param onSaveSuccessNavigateHome 저장 성공 — 바깥 스택을 애프터노트 홈까지 되감는다.
 */
@Composable
internal fun AfternoteEditorFlowHost(
    key: AfternoteRoute.EditorFlowRoute,
    onExitFlow: () -> Unit,
    onSaveSuccessNavigateHome: () -> Unit,
) {
    val editorViewModel =
        hiltViewModel<AfternoteEditorViewModel, AfternoteEditorViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) },
        )
    val flowStack = rememberNavBackStack(AfternoteRoute.EditorRoute)
    val boundary = remember(onExitFlow) { FeatureStackBoundary(onExitFlow) }
    val actions =
        remember(flowStack, boundary, editorViewModel, onSaveSuccessNavigateHome) {
            AfternoteEditorFlowLocalNavActions(
                flowStack = flowStack,
                boundary = boundary,
                onReceiversSelected = editorViewModel::onReceiversSelected,
                onSaveSuccessNavigateHome = onSaveSuccessNavigateHome,
            )
        }

    FeatureNavDisplay(
        backStack = flowStack,
        boundary = boundary,
        entryProvider =
            entryProvider {
                entry<AfternoteRoute.EditorRoute> {
                    AfternoteEditorNavigation(
                        editViewModel = editorViewModel,
                        onNavigateToMemorialPlaylist = actions::navigateToMemorialPlaylist,
                        onNavigateToSelectReceiver = actions::navigateToSelectReceiver,
                        onPopBackStack = actions::popBack,
                        onSaveSuccessNavigateHome = actions::popToAfternoteHome,
                    )
                }

                entry<AfternoteRoute.SelectReceiverRoute> {
                    // 폼에 이미 있는 수신자를 선택 상태로 열어 준다 — 선택 화면은 «추가분» 이 아니라
                    // 확정된 수신자 전체를 돌려주므로, 여는 시점의 폼이 곧 초기 선택이다 (#1426).
                    val editorUiState by editorViewModel.uiState.collectAsStateWithLifecycle()
                    AfternoteSelectReceiverNavigation(
                        preselectedReceiverIds = editorUiState.form.afternoteEditReceivers.map { it.id },
                        onPopBackStack = actions::popBack,
                        onReceiversConfirmed = actions::popBackWithSelectedReceivers,
                    )
                }

                entry<AfternoteRoute.MemorialPlaylistRoute> {
                    val editorUiState by editorViewModel.uiState.collectAsStateWithLifecycle()
                    MemorialPlaylistEntry(
                        songs = editorUiState.form.memorialPlaylistSongs,
                        onBackClick = actions::popBack,
                        onNavigateToAddSongScreen = actions::navigateToAddSong,
                        onClearAllSongs = editorViewModel::clearMemorialPlaylistSongs,
                        onRemoveSongs = editorViewModel::removeMemorialPlaylistSongs,
                    )
                }

                entry<AfternoteRoute.AddSongRoute> {
                    val addSongViewModel: AddSongViewModel = hiltViewModel()
                    AfternoteAddSongNavigation(
                        onPopBackStack = actions::popBack,
                        onSongsAdded = editorViewModel::addMemorialPlaylistSongs,
                        viewModel = addSongViewModel,
                    )
                }
            },
    )
}

/** 에디터 흐름 안에서만 의미가 있는 이동. 바깥 스택을 건드리는 둘은 콜백으로 위임한다. */
internal interface AfternoteEditorFlowNavActions {
    fun popBack()

    fun navigateToMemorialPlaylist()

    fun navigateToSelectReceiver()

    fun navigateToAddSong()

    /** 수신자 선택 완료 — 확정한 id 전체를 공유 ViewModel 에 남기고 에디터로 돌아간다 (#1426). */
    fun popBackWithSelectedReceivers(receiverIds: List<Long>)

    /** 저장 성공 — 흐름을 통째로 닫고 애프터노트 홈으로. */
    fun popToAfternoteHome()
}
