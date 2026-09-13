package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.afternote.presentation.navigation.AfternoteLightTheme
import com.afternote.feature.afternote.presentation.receiver.afternotelist.ReceiverAfternoteHomeEntry
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel
import com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistRoute
import com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistViewModel

/**
 * 수신 애프터노트(목록·상세·추억 플레이리스트)가 소유하는 로컬 Navigation 3 스택.
 *
 * 화면과 라우트의 소유는 애프터노트 피처에 있지만([com.afternote.core.ui.Route.Afternote] 그래프에
 * 넣지 않는 이유는 [ReceivedAfternoteRoute] 참고), 진입은 수신자 흐름에서 온다. Nav2 시절엔 세
 * 화면이 루트에 흩어져 있었고, 로컬 스택을 가지려면 담을 자리가 하나 필요해
 * [com.afternote.core.ui.Route.ReceivedAfternote] 를 세웠다.
 */
@Composable
public fun ReceivedAfternoteNavHost(
    boundary: FeatureStackBoundary,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(ReceivedAfternoteRoute.ListRoute)
    val actions = remember(backStack, boundary) { ReceivedAfternoteLocalNavActions(backStack, boundary) }

    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<ReceivedAfternoteRoute.ListRoute> {
                    AfternoteLightTheme {
                        ReceiverAfternoteHomeEntry(
                            navigateToDetail = actions::navigateToDetail,
                        )
                    }
                }

                entry<ReceivedAfternoteRoute.DetailRoute> { key ->
                    AfternoteLightTheme {
                        ReceivedAfternoteDetailRoute(
                            onNavigateBack = actions::popBack,
                            onNavigateToFullList = actions::navigateToList,
                            onNavigateToPlaylist = actions::navigateToMemorialPlaylist,
                            viewModel =
                                hiltViewModel<ReceivedAfternoteDetailViewModel, ReceivedAfternoteDetailViewModel.Factory>(
                                    creationCallback = { factory -> factory.create(key) },
                                ),
                        )
                    }
                }

                entry<ReceivedAfternoteRoute.MemorialPlaylistRoute> { key ->
                    AfternoteLightTheme {
                        val playlistViewModel =
                            hiltViewModel<ReceiverMemorialPlaylistViewModel, ReceiverMemorialPlaylistViewModel.Factory>(
                                creationCallback = { factory -> factory.create(key) },
                            )
                        ReceiverMemorialPlaylistRoute(viewModel = playlistViewModel, onBackClick = actions::popBack)
                    }
                }
            },
    )
}
