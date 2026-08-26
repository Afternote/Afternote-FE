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
    /**
     * 화면에 표시할 기록 날짜. **표시 전용이다** — 서버는 생성·수정 어느 쪽에서도 이 값을
     * 받지 않고 기록 날짜를 요청 시각으로 정한다 (#1008). 이어쓰기 프리필이 원래 날짜를
     * 채워 주는 자리라 상태 자체는 남긴다.
     */
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
    /** 이미지 업로드 진행 중 — 끝나기 전에 저장하면 이미지 없이 기록이 먼저 올라간다 (#716). */
    val isUploadingImage: Boolean = false,
    /** 이미지 업로드 실패 안내. 조용히 null 로 흡수하지 않는다 (#716). */
    val imageUploadError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
    /** 이어쓰기(기존 draft PATCH) 진입인지. 신규 작성이면 덮어쓸 기존 내용이 없다. */
    val isEditingDraft: Boolean = false,
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
        get() =
            submitState != SubmitState.InProgress &&
                !isDraftLoading &&
                // 업로드 중 저장하면 본문에 아직 안 들어간 이미지가 빠진 채 나간다 (#716).
                !isUploadingImage &&
                // 프리필이 실패했는데 저장하면, 보지 못한 기존 draft 내용을 빈 폼으로 PATCH 해
                // 덮어쓴다. 이어쓰기 진입에서 프리필이 실패한 동안은 저장을 막는다 (#716).
                !(isEditingDraft && draftLoadError != null)

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
