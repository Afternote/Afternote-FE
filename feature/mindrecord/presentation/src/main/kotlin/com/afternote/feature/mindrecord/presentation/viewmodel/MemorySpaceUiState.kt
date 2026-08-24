package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.annotation.StringRes
import com.afternote.feature.mindrecord.presentation.model.memoryspace.MemoryItem

sealed interface MemorySpaceUiState {
    data object Loading : MemorySpaceUiState

    /** [memories] 가 비어 있으면 아직 기록이 없는 사용자다 — 화면이 빈 상태 안내를 띄운다. */
    data class Success(
        val memories: List<MemoryItem>,
    ) : MemorySpaceUiState

    /**
     * 조회 실패. 표시 문구는 [messageRes] 하나로만 운반한다 — 예외 원문(`Throwable.message`)은 UI 에
     * 싣지 않는다. 오프라인이면 `Unable to resolve host "afternote.kro.kr"…` 같은 영어 원문이,
     * 5xx 면 서버 내부 문구가 그대로 화면에 뜬다 (`ApiException` 도 `message` 는 로그 전용이고
     * 사용자 노출용으로 `serverMessage` 를 따로 둔다 — 같은 규칙을 `AfternoteDetailUiState.Error` 가 쓴다).
     *
     * 이 화면은 조회만 하므로 서버가 4xx 로 안내할 사용자 교정 사항 자체가 없다. 그래서
     * `serverMessage` 선별 없이 정적 리소스 하나로 고정한다.
     */
    data class Error(
        @param:StringRes val messageRes: Int,
    ) : MemorySpaceUiState
}
