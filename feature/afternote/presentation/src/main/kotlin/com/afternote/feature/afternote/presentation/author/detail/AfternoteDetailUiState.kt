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
 * - [Error] 상세 데이터 조회 실패. 표시 문구는 [Error.messageRes] 하나로만 운반한다.
 *   예외 원문(`Throwable.message`)은 UI 에 싣지 않는다 — 서버 5xx 본문에 내부 SQL 이,
 *   역직렬화 실패 메시지에 응답 원문 발췌·DTO 클래스명이 섞여 오기 때문이다
 *   (`ApiException` 도 `message` 는 로그 전용이고 사용자 노출용으로 `serverMessage` 를 따로 둔다).
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
        @param:StringRes val messageRes: Int,
    ) : AfternoteDetailDeleteResult
}
