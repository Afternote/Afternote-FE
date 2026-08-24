package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.util.isHtmlBlank
import java.time.LocalDate

data class DiaryWriteUiState(
    val title: String = "",
    val content: String = "",
    val mood: TodayMood? = null,
    val date: LocalDate = LocalDate.now(),
    val imageUrl: String? = null,
    /** `GET /users/receivers` 로 불러온 내 수신인 목록. 로드 실패 시 빈 목록 (작성은 가능). */
    val receivers: List<Receiver> = emptyList(),
    /** 수신자 선택 바텀시트에서 고른 수신자 ID. 등록 payload 의 `receiverIds` 로 전송. */
    val selectedReceiverIds: Set<Long> = emptySet(),
    /** 임시저장 이어쓰기 — draft 프리필 로딩 중 여부. */
    val isDraftLoading: Boolean = false,
    /** draft 프리필 완료 플래그. 에디터(content) 재시드 트리거로 사용. */
    val draftLoaded: Boolean = false,
    val draftLoadError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
    /** 툴바 "임시저장 N" 표시값. `null` 은 아직 모름(조회 중·실패) (#769). */
    val draftCount: Int? = null,
) {
    /** 정식 등록 조건 — 제목·본문·기분이 모두 있어야 한다. */
    val canSubmit: Boolean
        get() = missingForSubmit() == null && isReady

    /**
     * 임시저장 조건.
     *
     * «미완성 보존» 이 목적이라 제목·본문 중 하나만 있어도 저장하려 했지만, **서버가
     * 임시저장에도 제목·본문·기분을 모두 요구한다** — 실서버 실측(2026-08-24):
     *
     * ```
     * POST /diary {"title":"제목만","content":"","isDraft":true,…}   → 400 "내용은 필수입니다."
     * POST /diary {"title":"","content":"<p>본문만</p>",…}           → 400 "제목은 필수입니다."
     * POST /diary {"title":"제목","content":"<p>본문</p>"}            → 400 "오늘의 기분은 필수입니다."
     * ```
     *
     * 보내면 400 이 되는 조건을 «저장 가능» 으로 표시하면 버튼이 고장 난 것과 같아진다.
     * 서버가 `isDraft=true` 에서 검증을 완화해 주기 전까지는 같은 조건을 요구한다 (#1065).
     *
     * 정식 등록과 갈리는 지점은 남는다 — 실패 사유를 각각 다른 문구로 알린다.
     */
    val canSaveDraft: Boolean
        get() = missingForDraft() == null && isReady

    /** 임시저장을 막는 첫 번째 누락 항목 (없으면 null). 현재는 정식 등록과 같은 세 가지다. */
    fun missingForDraft(): Int? = missingForSubmit()

    private val isReady: Boolean
        get() = submitState != SubmitState.InProgress && !isDraftLoading

    /**
     * 등록을 막는 첫 번째 누락 항목 (없으면 null).
     *
     * 회색 비활성 버튼만으로는 무엇이 빠졌는지 알 수 없어 고장과 구분되지 않았다 (#722).
     * 본문은 직렬화된 HTML 이라 `isNotBlank()` 로는 빈 문단을 잡지 못한다.
     */
    fun missingForSubmit(): Int? =
        when {
            title.isBlank() -> R.string.mindrecord_write_diary_missing_title
            content.isHtmlBlank() -> R.string.mindrecord_write_diary_missing_content
            mood == null -> R.string.mindrecord_write_diary_missing_mood
            else -> null
        }

    val selectedReceivers: List<Receiver>
        get() = receivers.filter { it.receiverId in selectedReceiverIds }
}
