package com.afternote.feature.afternote.presentation.author.detail

import androidx.annotation.StringRes

/**
 * 애프터노트 상세 화면 UI 상태.
 *
 * - [Loading] 상세 데이터 로드 진행 중. 최초 진입 및 재로드 시 진입.
 * - [Success] 상세 데이터 조회 성공. 작성자 표시명·삭제 진행 플래그·타입별 [DetailContentUiModel] 을 보관한다.
 *   작성자 표시명은 홈 summary 에서 별도 조회되므로 로딩 경합 시 빈 문자열로 시작해 추후 copy 된다.
 *   삭제 결과(성공/실패)는 일회성 이벤트로 [AfternoteDetailEvent] 채널을 통해 흘러간다 (UI는 [com.afternote.core.ui.ObserveAsEvents] 로 수집).
 * - [Error] 상세 데이터 조회 실패. UI는 [Error.rawMessage](서버 메시지 등)를 우선 사용하고
 *   비어있으면 [Error.messageRes] 를 [androidx.compose.ui.res.stringResource] 로 변환한다.
 *   [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel] 의
 *   `error: String?` + `errorRes: Int?` 페어 패턴과 동일한 i18n 분리.
 */
sealed interface AfternoteDetailUiState {
    data object Loading : AfternoteDetailUiState

    data class Success(
        val detailId: Long,
        val authorDisplayName: String = "",
        val isDeleting: Boolean = false,
        val contentUiModel: DetailContentUiModel,
    ) : AfternoteDetailUiState

    data class Error(
        val rawMessage: String? = null,
        @param:StringRes val messageRes: Int? = null,
    ) : AfternoteDetailUiState
}

/**
 * 상세 화면 일회성 이벤트.
 *
 * 영속 상태(`StateFlow`)가 아니므로 [androidx.compose.runtime.LaunchedEffect] 키잉으로 받지 않고
 * [com.afternote.core.ui.ObserveAsEvents] 로만 수집해 백그라운드 재발사·중복 처리를 방지한다.
 */
sealed interface AfternoteDetailEvent {
    data class DeleteSucceeded(
        val id: Long,
    ) : AfternoteDetailEvent

    data class DeleteFailed(
        val rawMessage: String? = null,
        @param:StringRes val messageRes: Int? = null,
    ) : AfternoteDetailEvent
}
