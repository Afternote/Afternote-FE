package com.afternote.feature.afternote.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.detail.account.AccountDetailScreen
import com.afternote.feature.afternote.presentation.shared.detail.DesignPendingDetailContent
import com.afternote.feature.afternote.presentation.shared.detail.DetailLoadErrorContent

/** 상세 상태의 모든 렌더 분기. 상태 수집과 일회성 결과 소비는 Navigation이 담당한다. */
@Composable
internal fun AfternoteDetailContent(
    uiState: AfternoteDetailUiState,
    onIntent: (AfternoteDetailIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long, AfternoteType) -> Unit,
    onVideoClick: (String) -> Unit,
) {
    when (val state = uiState) {
        AfternoteDetailUiState.Loading -> {
            LoadingBody()
        }

        is AfternoteDetailUiState.Error -> {
            DetailLoadErrorContent(
                messageRes = state.messageRes,
                onBackClick = onNavigateBack,
                onRetryClick = { onIntent(AfternoteDetailIntent.Retry) },
            )
        }

        is AfternoteDetailUiState.Success -> {
            AfternoteDetailSuccessContent(
                state = state,
                snackbarHostState = snackbarHostState,
                onBackClick = onNavigateBack,
                onNavigateToEditor = onNavigateToEditor,
                onDeleteConfirm = { onIntent(AfternoteDetailIntent.Delete) },
                onVideoClick = onVideoClick,
            )
        }
    }
}

@Composable
private fun AfternoteDetailSuccessContent(
    state: AfternoteDetailUiState.Success,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onNavigateToEditor: (itemId: Long, type: AfternoteType) -> Unit,
    onDeleteConfirm: () -> Unit,
    onVideoClick: (String) -> Unit,
) {
    Box {
        when (val model = state.contentUiModel) {
            is DetailContentUiModel.SocialNetwork -> {
                AccountDetailScreen(
                    content = model.content,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onBackClick,
                    onEditClick = {
                        onNavigateToEditor(state.detailId, model.type)
                    },
                    onDeleteConfirm = onDeleteConfirm,
                )
            }

            is DetailContentUiModel.Business -> {
                AccountDetailScreen(
                    content = model.content,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onBackClick,
                    onEditClick = {
                        onNavigateToEditor(state.detailId, model.type)
                    },
                    onDeleteConfirm = onDeleteConfirm,
                )
            }

            is DetailContentUiModel.Gallery -> {
                GalleryDetailScreen(
                    content = model.content,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onBackClick,
                    onEditClick = {
                        onNavigateToEditor(state.detailId, model.type)
                    },
                    onDeleteConfirm = onDeleteConfirm,
                )
            }

            is DetailContentUiModel.Memorial -> {
                MemorialDetailScreen(
                    content = model.content,
                    userName = state.authorDisplayName,
                    snackbarHostState = snackbarHostState,
                    onBackClick = onBackClick,
                    onEditClick = {
                        onNavigateToEditor(state.detailId, model.type)
                    },
                    onDeleteConfirm = onDeleteConfirm,
                    onVideoClick = onVideoClick,
                )
            }

            DetailContentUiModel.Unimplemented -> {
                DesignPendingDetailContent(onBackClick = onBackClick)
            }
        }

        if (state.isDeleting) {
            DeleteInProgressOverlay()
        }
    }
}

/**
 * 삭제 진행([AfternoteDetailUiState.Success.isDeleting]) 동안 상세 화면 위에 겹쳐 그리는 오버레이.
 *
 * 반투명 스크림 + 중앙 진행 인디케이터. 중복 delete 호출은 ViewModel 이 이미 가드하므로
 * 여기서는 시각 표시와 입력 차단만 담당한다.
 *
 * 포인터 차단만으로는 부족하다 — 스크림에 semantics 가 없으면 접근성 트리에서 잘리지 않아
 * 아래 상세 화면의 뒤로·수정·삭제 버튼이 스크린리더로는 그대로 탐색·활성화된다.
 * 그래서 스크림 자체를 병합 노드로 만들어 모달성을 유지한다.
 */
@Composable
private fun DeleteInProgressOverlay() {
    val deletingDescription = stringResource(R.string.afternote_detail_deleting)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AfternoteDesign.colors.black.copy(alpha = 0.3f))
                .pointerInput(Unit) { detectTapGestures {} }
                .semantics(mergeDescendants = true) {
                    contentDescription = deletingDescription
                    liveRegion = LiveRegionMode.Polite
                },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
