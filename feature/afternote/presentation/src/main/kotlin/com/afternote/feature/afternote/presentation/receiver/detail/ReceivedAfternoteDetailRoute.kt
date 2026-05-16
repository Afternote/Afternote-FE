package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.afternote.presentation.author.navigation.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadingContent

/**
 * 수신 애프터노트 상세 Stateful Route.
 *
 * Now in Android 가이드의 Route + Screen 분리 패턴을 따른다. UiState 분기 후 카테고리에 따라
 * Stateless Screen([SocialNetworkReceivedDetailScreen] / [GalleryReceivedDetailScreen]) 으로 위임한다.
 * 추모(MEMORIAL) 카테고리는 디자인 미정이므로 발신자와 동일하게 [DesignPendingDetailContent] 폴백.
 *
 * NavHost 통합은 receiver 네비게이션이 도입될 때 별도 라우트로 연결.
 */
@Composable
fun ReceivedAfternoteDetailRoute(
    onBack: () -> Unit,
    viewModel: ReceivedAfternoteDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        ReceivedAfternoteDetailUiState.Loading -> {
            DetailLoadingContent()
        }

        is ReceivedAfternoteDetailUiState.Error -> {
            DesignPendingDetailContent(onBackClick = onBack)
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

                ReceivedDetailContentUiModel.MemorialPending,
                ReceivedDetailContentUiModel.Unknown,
                -> {
                    DesignPendingDetailContent(onBackClick = onBack)
                }
            }
        }
    }
}
