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
    /**
     * 이어쓸 임시저장 본문이 도착했는지.
     *
     * 에디터가 외부 값을 초기 시드로만 받으므로, 화면이 이 플래그로 에디터를 재생성해 본문을
     * 다시 싣는다. 일기 화면의 `draftLoaded` 와 같은 역할이다 (#923).
     */
    val draftLoaded: Boolean = false,
    val answer: String = "",
    val imageUrl: String? = null,
    val isQuestionLoading: Boolean = true,
    val questionLoadError: UiText? = null,
    /** 이어쓸 임시저장 본문을 불러오는 중 (#923). */
    val isResumingDraft: Boolean = false,
    val submitState: SubmitState = SubmitState.Idle,
    /** 툴바 "임시저장 N" 표시값. `null` 은 아직 모름(조회 중·실패) (#769). */
    val draftCount: Int? = null,
) {
    /**
     * `questionId` 유무는 여기서 보지 않는다. 조건에 넣으면 오늘 질문 조회가 실패했을 때
     * 저장 버튼이 그냥 죽어 있어 원인을 알 수 없다 (#565). 대신 [DailyQuestionWriteViewModel.submit]
     * 이 눌린 시점에 사유를 알리고 조회를 재시도한다.
     */
    val canSubmit: Boolean
        get() =
            // 리치 에디터는 빈 문단도 `<p></p>`·`<br>` 로 직렬화한다. isNotBlank() 로 판정하면
            // 화면이 비어 있어도 통과해 빈 답변이 저장되고 작성 화면이 pop 됐다 (#722).
            !answer.isHtmlBlank() &&
                !isQuestionLoading &&
                // 이어쓸 본문이 도착하기 전에 저장하면 draftId 가 아직 null 이라 POST 로 나가고,
                // 서버가 questionId upsert 라 기존 임시저장 본문이 덮인다 — #923 과 같은 유실이다.
                !isResumingDraft &&
                submitState != SubmitState.InProgress
}

sealed interface SubmitState {
    data object Idle : SubmitState

    data object InProgress : SubmitState

    data object Succeeded : SubmitState

    data class Failed(
        val message: UiText,
    ) : SubmitState
}
