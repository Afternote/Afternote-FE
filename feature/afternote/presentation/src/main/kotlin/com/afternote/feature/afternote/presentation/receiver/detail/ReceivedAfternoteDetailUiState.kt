package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.annotation.StringRes

sealed interface ReceivedAfternoteDetailUiState {
    data object Loading : ReceivedAfternoteDetailUiState

    data class Success(
        val detailId: Long,
        val contentUiModel: ReceivedDetailContentUiModel,
    ) : ReceivedAfternoteDetailUiState

    /**
     * @param canRetry 같은 상세를 다시 조회할 수 있는 실패인지. 조회 실패(네트워크·5xx 등)는 `true`,
     *   라우트 인자에 상세 ID 가 없는 실패는 재요청해도 결과가 같으므로 `false`.
     */
    data class Error(
        @param:StringRes val messageRes: Int? = null,
        val canRetry: Boolean = false,
    ) : ReceivedAfternoteDetailUiState
}
