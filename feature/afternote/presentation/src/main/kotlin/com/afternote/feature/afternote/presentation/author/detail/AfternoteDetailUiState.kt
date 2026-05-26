package com.afternote.feature.afternote.presentation.author.detail

import androidx.annotation.StringRes

/**
 * 애프터노트 상세 화면 UI 상태.
 *
 * - [Loading] 상세 데이터 로드 진행 중. 최초 진입 및 재로드 시 진입.
 * - [Success] 상세 데이터 조회 성공. 작성자 표시명·삭제 진행 플래그·타입별 [DetailContentUiModel] 을 보관한다.
 *   작성자 표시명은 홈 summary 에서 별도 조회되므로 로딩 경합 시 빈 문자열로 시작해 추후 copy 된다.
 *   삭제 결과(성공/실패)는 [Success.deleteResult] 에 흡수 — UI 가 LaunchedEffect 로 소비 후
 *   [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel.onDeleteResultConsumed] 로 reset.
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
        /** 삭제 결과 신호 — UI 가 LaunchedEffect 로 소비 후 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel.onDeleteResultConsumed] 로 reset. */
        val deleteResult: AfternoteDetailDeleteResult? = null,
    ) : AfternoteDetailUiState

    data class Error(
        val rawMessage: String? = null,
        @param:StringRes val messageRes: Int? = null,
    ) : AfternoteDetailUiState
}

/**
 * 삭제 액션 결과를 UI state 에 흡수하기 위한 sealed wrapper — 성공/실패 상호 배타 보장.
 *
 * Google 공식 가이드 — ViewModel events should always result in a UI state update.
 * Channel + ObserveAsEvents 패턴은 producer(VM) 가 consumer(UI) 보다 오래 살 때 delivery 보장 X
 * (configuration change · process death · 분할 화면 일관성 결함). nullable 필드 흡수로 통일.
 */
sealed interface AfternoteDetailDeleteResult {
    data class Succeeded(
        val id: Long,
    ) : AfternoteDetailDeleteResult

    data class Failed(
        val rawMessage: String? = null,
        @param:StringRes val messageRes: Int? = null,
    ) : AfternoteDetailDeleteResult
}
