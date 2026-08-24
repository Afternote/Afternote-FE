package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.util.isHtmlBlank

data class DailyQuestionWriteUiState(
    val questionId: Long? = null,
    /** "Day N" 배너 표기용 — 오늘의 질문이 서비스 기준 몇 일차인지. */
    val questionDay: Int? = null,
    val questionContent: String = "",
    /** 오늘 이미 임시저장된 답변 레코드 ID — null 이 아니면 제출 시 POST 대신 PATCH 로 전환한다. */
    val draftId: Long? = null,
    val answer: String = "",
    val imageUrl: String? = null,
    val isQuestionLoading: Boolean = true,
    val questionLoadError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    /**
     * `questionId` 유무는 여기서 보지 않는다. 조건에 넣으면 오늘 질문 조회가 실패했을 때
     * 저장 버튼이 그냥 죽어 있어 원인을 알 수 없다 (#565). 대신 [DailyQuestionWriteViewModel.submit]
     * 이 눌린 시점에 사유를 알리고 조회를 재시도한다.
     */
    val canSubmit: Boolean
        // 리치 에디터는 빈 문단도 `<p></p>`·`<br>` 로 직렬화한다. isNotBlank() 로 판정하면
        // 화면이 비어 있어도 통과해 빈 답변이 저장되고 작성 화면이 pop 됐다 (#722).
        get() = !answer.isHtmlBlank() && !isQuestionLoading && submitState != SubmitState.InProgress
}

sealed interface SubmitState {
    data object Idle : SubmitState

    data object InProgress : SubmitState

    data object Succeeded : SubmitState

    data class Failed(
        val message: UiText,
    ) : SubmitState
}
