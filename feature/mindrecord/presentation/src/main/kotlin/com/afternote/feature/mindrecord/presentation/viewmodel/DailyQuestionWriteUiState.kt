package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText

data class DailyQuestionWriteUiState(
    val questionId: Long? = null,
    /** "Day N" 배너 표기용 — 오늘의 질문이 서비스 기준 몇 일차인지. */
    val questionDay: Int? = null,
    val questionContent: String = "",
    /**
     * 수정 대상 답변 레코드 ID — null 이 아니면 제출 시 POST 대신 PATCH 로 전환한다.
     *
     * 오늘의 임시저장 이어쓰기와 목록의 "수정하기" 가 같은 값을 쓴다 (#582).
     */
    val draftId: Long? = null,
    val answer: String = "",
    /**
     * 본문이 서버에서 채워졌는지. 리치 에디터는 [answer] 를 **초기 시드로만** 읽으므로,
     * 비동기 프리필이 끝난 뒤 에디터를 다시 만들어야 내용이 보인다 (#582).
     */
    val contentLoaded: Boolean = false,
    val imageUrl: String? = null,
    val isQuestionLoading: Boolean = true,
    val questionLoadError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    val canSubmit: Boolean
        // 수정 모드는 오늘 질문을 부르지 않아 questionId 가 없다 — 대상 레코드가 있으면 저장할 수 있다.
        get() = (questionId != null || draftId != null) && answer.isNotBlank() && submitState != SubmitState.InProgress
}

sealed interface SubmitState {
    data object Idle : SubmitState

    data object InProgress : SubmitState

    data object Succeeded : SubmitState

    data class Failed(
        val message: UiText,
    ) : SubmitState
}
