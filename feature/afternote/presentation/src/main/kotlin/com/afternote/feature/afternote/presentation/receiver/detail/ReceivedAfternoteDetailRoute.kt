package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.afternote.presentation.author.navigation.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadErrorContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadingContent

/**
 * 수신 애프터노트 상세 Stateful Route.
 *
 * Now in Android 가이드의 Route + Screen 분리 패턴을 따른다. UiState 분기 후 카테고리에 따라
 * Stateless Screen([SocialNetworkReceivedDetailScreen] / [GalleryReceivedDetailScreen] /
 * [MemorialReceivedDetailScreen]) 으로 위임한다. 추억(MEMORIAL) 의 "추억 플레이리스트" 진입은
 * [onNavigateToPlaylist] 로 위임한다 (#274).
 *
 * 조회 실패는 [DetailLoadErrorContent] 로 간다 — 발신 상세와 같은 실패 화면이며 재조회 액션을 준다.
 * [DesignPendingDetailContent] 는 성공 응답의 미구현 카테고리(BUSINESS·ESTATE·Unknown) 전용이다.
 * 실패를 이쪽으로 보내면 서버·네트워크 오류가 "디자인 예정" 으로 표시되고 재시도 수단도 사라진다 (#713).
 */
@Composable
fun ReceivedAfternoteDetailRoute(
    onBack: () -> Unit,
    onNavigateToPlaylist: (afternoteId: String) -> Unit = {},
    viewModel: ReceivedAfternoteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        ReceivedAfternoteDetailUiState.Loading -> {
            DetailLoadingContent()
        }

        is ReceivedAfternoteDetailUiState.Error -> {
            DetailLoadErrorContent(
                messageRes = state.messageRes,
                onBackClick = onBack,
                onRetryClick = if (state.canRetry) viewModel::retry else null,
            )
        }

        is ReceivedAfternoteDetailUiState.Success -> {
            when (val model = state.contentUiModel) {
                is ReceivedDetailContentUiModel.SocialNetwork -> {
                    SocialNetworkReceivedDetailScreen(
                        content = model.content,
                        onBackClick = onBack,
                    )
                }

                is ReceivedDetailContentUiModel.Gallery -> {
                    GalleryReceivedDetailScreen(
                        content = model.content,
                        onBackClick = onBack,
                    )
                }

                is ReceivedDetailContentUiModel.Memorial -> {
                    MemorialReceivedDetailScreen(
                        senderName = model.content.senderName,
                        leaveMessage = model.content.leaveMessage,
                        albumCovers = model.content.albumCovers,
                        songCount = model.content.songCount,
                        memorialVideoUrl = model.content.memorialVideoUrl,
                        memorialThumbnailUrl = model.content.memorialThumbnailUrl,
                        onNavigateToPlaylist = { onNavigateToPlaylist(state.detailId.toString()) },
                        onBackClick = onBack,
                    )
                }

                ReceivedDetailContentUiModel.Unimplemented,
                ReceivedDetailContentUiModel.Unknown,
                -> {
                    DesignPendingDetailContent(onBackClick = onBack)
                }
            }
        }
    }
}
