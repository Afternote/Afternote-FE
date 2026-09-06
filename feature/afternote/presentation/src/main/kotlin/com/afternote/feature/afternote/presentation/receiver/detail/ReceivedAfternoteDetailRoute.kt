package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.feature.afternote.presentation.shared.detail.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.shared.detail.DetailLoadErrorContent

/**
 * 수신 애프터노트 상세 Stateful Route.
 *
 * Now in Android 가이드의 Route + Screen 분리 패턴을 따른다. UiState 분기 후 카테고리에 따라
 * Stateless Screen([SocialNetworkReceivedDetailScreen] / [GalleryReceivedDetailScreen] /
 * [MemorialReceivedDetailScreen]) 으로 위임한다. 추억(MEMORIAL) 의 "추억 플레이리스트" 진입은
 * [onNavigateToPlaylist] 로 위임한다 (#274).
 *
 * 하단 "애프터노트 확인하기" 는 [onNavigateToFullList] 로 위임한다 — 수신자가 이 발신자에게서
 * 받은 애프터노트 전체 목록([com.afternote.feature.afternote.presentation.receiver.navigation.ReceivedAfternoteRoute.ListRoute])
 * 이 목적지다. 파라미터 이름이 가리키는 "전체 목록" 이 수신자 흐름에 그것 하나뿐이다 (#777).
 *
 * 조회 실패는 [DetailLoadErrorContent] 로 간다 — 발신 상세와 같은 실패 화면이며 재조회 액션을 준다.
 * [DesignPendingDetailContent] 는 성공 응답의 미구현 카테고리(BUSINESS·ESTATE·Unknown) 전용이다.
 * 실패를 이쪽으로 보내면 서버·네트워크 오류가 "디자인 예정" 으로 표시되고 재시도 수단도 사라진다 (#713).
 */
@Composable
fun ReceivedAfternoteDetailRoute(
    onNavigateBack: () -> Unit,
    onNavigateToFullList: () -> Unit,
    onNavigateToPlaylist: (afternoteId: Long) -> Unit,
    viewModel: ReceivedAfternoteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 화면을 떠났다 돌아오면 상세를 다시 조회한다 — 백스택에 살아 있는 동안 옛 값이 남지
    // 않게 한다 (#701). ON_RESUME 은 화면 off/on·홈 버튼 복귀에서도 발화하므로 로딩을
    // 방출하지 않는 refreshOnReturn() 을 쓴다. 첫 진입의 ON_RESUME 스킵(진입은 init 로드가
    // 담당)과 실행 중 로드와의 중복 차단은 VM 이 판단한다 — 결선부는 무조건 부른다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    when (val state = uiState) {
        ReceivedAfternoteDetailUiState.Loading -> {
            LoadingBody()
        }

        is ReceivedAfternoteDetailUiState.Error -> {
            DetailLoadErrorContent(
                messageRes = state.messageRes,
                onBackClick = onNavigateBack,
                onRetryClick = viewModel::retry,
            )
        }

        is ReceivedAfternoteDetailUiState.Success -> {
            when (val model = state.contentUiModel) {
                is ReceivedDetailContentUiModel.SocialNetwork -> {
                    SocialNetworkReceivedDetailScreen(
                        content = model.content,
                        onBackClick = onNavigateBack,
                    )
                }

                is ReceivedDetailContentUiModel.Gallery -> {
                    GalleryReceivedDetailScreen(
                        content = model.content,
                        onBackClick = onNavigateBack,
                    )
                }

                is ReceivedDetailContentUiModel.Memorial -> {
                    MemorialReceivedDetailScreen(
                        senderName = model.content.senderName,
                        messageBlocks = model.content.messageBlocks,
                        albumCovers = model.content.albumCovers,
                        songCount = model.content.songCount,
                        memorialVideoUrl = model.content.memorialVideoUrl,
                        memorialThumbnailUrl = model.content.memorialThumbnailUrl,
                        onNavigateToFullList = onNavigateToFullList,
                        onNavigateToPlaylist = { onNavigateToPlaylist(state.detailId) },
                        onBackClick = onNavigateBack,
                    )
                }

                ReceivedDetailContentUiModel.Unimplemented -> {
                    DesignPendingDetailContent(onBackClick = onNavigateBack)
                }
            }
        }
    }
}
