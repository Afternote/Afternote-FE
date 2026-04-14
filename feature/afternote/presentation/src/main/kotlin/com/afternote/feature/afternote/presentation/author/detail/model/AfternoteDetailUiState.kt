package com.afternote.feature.afternote.presentation.author.detail.model

import com.afternote.feature.afternote.domain.model.author.Detail

/**
 * 애프터노트 상세 화면 UI 상태.
 *
 * - [Loading] 상세 데이터 로드 진행 중. 최초 진입 및 재로드 시 진입.
 * - [Success] 상세 데이터 조회 성공. 작성자 표시명·삭제 하위 상태를 함께 보관한다.
 *   작성자 표시명은 홈 summary 에서 별도 조회되므로 로딩 경합 시 빈 문자열로 시작해 추후 copy 된다.
 * - [Error] 상세 데이터 조회 실패. 에러 메시지를 보유한다.
 */
sealed interface AfternoteDetailUiState {
    data object Loading : AfternoteDetailUiState

    data class Success(
        val detail: Detail,
        val authorDisplayName: String = "",
        val deleteState: AfternoteDeleteState = AfternoteDeleteState.Idle,
    ) : AfternoteDetailUiState

    data class Error(
        val message: String,
    ) : AfternoteDetailUiState
}

/**
 * 상세 화면 내 삭제 작업의 하위 상태.
 *
 * [Succeeded]/[Failed] 는 UI 가 소비(`AfternoteDetailEvent.DeleteResultConsumed`) 하면 [Idle] 로 복귀한다.
 */
sealed interface AfternoteDeleteState {
    data object Idle : AfternoteDeleteState

    data object InProgress : AfternoteDeleteState

    data object Succeeded : AfternoteDeleteState

    data class Failed(
        val message: String,
    ) : AfternoteDeleteState
}
