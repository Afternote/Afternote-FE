package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadErrorContent
import com.afternote.feature.afternote.presentation.author.navigation.afternoteComposable
import com.afternote.feature.afternote.presentation.receiver.afternotelist.ReceiverAfternoteHomeEntry
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceivedAfternoteRoute
import com.afternote.feature.afternote.presentation.receiver.playlist.MemorialPlaylistScreen
import com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistUiState
import com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistViewModel

/**
 * 수신 애프터노트 목록·상세·추억 플레이리스트를 루트 NavHost 에 등록한다.
 *
 * [com.afternote.core.ui.Route.Afternote] 그래프에 넣지 않는다 — 그 그래프의 시작점은 발신자용
 * 지문 관문이고, 수신자는 로그인 사용자가 아니라 그 관문을 지나지 않는다. 수신자 마음의 기록이
 * [com.afternote.core.ui.Route.ReceiverMindRecord] 를 제 피처 그래프에서 등록하는 것과 같은 형태다.
 *
 * 이 화면들로의 진입은 수신자 흐름([com.afternote.core.ui.Route.Receiver])에서 오지만, 화면과
 * 라우트의 소유는 애프터노트 피처에 둔다 (#1461).
 */
fun NavGraphBuilder.receivedAfternoteNavGraph(actions: ReceivedAfternoteNavActions) {
    afternoteComposable<ReceivedAfternoteRoute.ListRoute> {
        ReceiverAfternoteHomeEntry(
            navigateToDetail = actions::navigateToDetail,
        )
    }

    afternoteComposable<ReceivedAfternoteRoute.DetailRoute> {
        ReceivedAfternoteDetailRoute(
            onNavigateBack = actions::popBack,
            onNavigateToFullList = actions::navigateToList,
            onNavigateToPlaylist = actions::navigateToMemorialPlaylist,
        )
    }

    afternoteComposable<ReceivedAfternoteRoute.MemorialPlaylistRoute> {
        val playlistViewModel: ReceiverMemorialPlaylistViewModel = hiltViewModel()
        val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
        when (val state = playlistUiState) {
            ReceiverMemorialPlaylistUiState.Loading -> {
                LoadingBody()
            }

            is ReceiverMemorialPlaylistUiState.Error -> {
                DetailLoadErrorContent(
                    messageRes = state.messageRes,
                    onBackClick = actions::popBack,
                    onRetryClick = playlistViewModel::retry,
                )
            }

            is ReceiverMemorialPlaylistUiState.Success -> {
                MemorialPlaylistScreen(
                    senderName = state.senderName,
                    songs = state.songs,
                    onBackClick = actions::popBack,
                )
            }
        }
    }
}
