package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.util.isHtmlBlank

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
    /**
     * 이어쓸 임시저장 본문이 도착했는지.
     *
     * 에디터가 외부 값을 초기 시드로만 받으므로, 화면이 이 플래그로 에디터를 재생성해 본문을
     * 다시 싣는다. 일기 화면의 `draftLoaded` 와 같은 역할이다 (#923).
     */
    val draftLoaded: Boolean = false,
    val answer: String = "",
    /**
     * 본문이 서버에서 채워졌는지. 리치 에디터는 [answer] 를 **초기 시드로만** 읽으므로,
     * 비동기 프리필이 끝난 뒤 에디터를 다시 만들어야 내용이 보인다 (#582).
     */
    val contentLoaded: Boolean = false,
    val isQuestionLoading: Boolean = true,
    val questionLoadError: UiText? = null,
    /** 이어쓸 임시저장 본문을 불러오는 중 (#923). */
    val isResumingDraft: Boolean = false,
    /**
     * 임시저장 이어쓰기 조회 실패.
     *
     * «불러오지 못함» 을 «임시저장 없음» 과 갈라야 하는 이유는 저장이 upsert 라서다 — 실패를
     * 삼키면 사용자는 빈 화면을 «아직 임시저장이 없다» 로 읽고, 그대로 저장하는 순간 서버에
     * 남아 있던 임시저장이 덮인다. 일기 화면은 같은 실패를 draftLoadError 로 이미 드러낸다 (#1018).
     */
    val draftResumeError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
    /** 이미지 업로드 진행 중 — 끝나기 전에 저장하면 이미지 없이 기록이 먼저 올라간다 (#716). */
    val isUploadingImage: Boolean = false,
    /** 이미지 업로드 실패 안내. 조용히 null 로 흡수하지 않는다 (#716). */
    val imageUploadError: UiText? = null,
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
            // `isNotBlank()` 가 아니라 [isHtmlBlank] 다. 에디터는 아무것도 입력하지 않아도
            // `<p></p>`·`<br>` 를 내보내므로 문자열 공백 판정으로는 «화면이 비었는지» 를 알 수 없다 —
            // 그 값으로 저장이 열리면 빈 답변이 새로 생기거나, 이어쓰던 draft 본문이 빈
            // HTML 로 PATCH 돼 덮인다. 실제로 빈 답변이 저장되고 작성 화면이 pop 됐다
            // (#722 · #1018 리뷰). 같은 판정을 [DailyQuestionWriteViewModel.submit] 도 쓴다 —
            // 하단 툴바 임시저장이 `enabled` 없는 clickable 이라 이 조건을 우회하기 때문이다.
            !answer.isHtmlBlank() &&
                // 수정·이어쓰기 모드는 오늘 질문을 부르지 않아 questionId 가 없다 — 대상 레코드가
                // 있으면 PATCH 로 나가므로 질문 조회를 기다릴 이유가 없다 (#582·#770).
                (draftId != null || !isQuestionLoading) &&
                // 이어쓸 본문이 도착하기 전에 저장하면 draftId 가 아직 null 이라 POST 로 나가고,
                // 서버가 questionId upsert 라 기존 임시저장 본문이 덮인다 — #923 과 같은 유실이다.
                !isResumingDraft &&
                submitState != SubmitState.InProgress &&
                // 업로드 중 저장하면 본문에 아직 안 들어간 이미지가 빠진 채 나간다 (#716).
                !isUploadingImage &&
                // 이어쓸 본문을 못 불러온 동안은 막는다. 경고만 띄우고 저장을 열어 두면
                // 빈 에디터(`<p></p>`)도 isNotBlank() 라 버튼이 살아 있고, draftId 가 null 인
                // 채 POST 로 나가 서버 upsert 가 기존 임시저장을 빈 본문으로 덮는다 —
                // 이 PR 이 막으려던 바로 그 유실이다 (#1018 리뷰). 일기 화면의
                // `!(isEditingDraft && draftLoadError != null)` 과 같은 성질이다.
                draftResumeError == null
}

sealed interface SubmitState {
    data object Idle : SubmitState

    data object InProgress : SubmitState

    data object Succeeded : SubmitState

    data class Failed(
        val message: UiText,
    ) : SubmitState
}
