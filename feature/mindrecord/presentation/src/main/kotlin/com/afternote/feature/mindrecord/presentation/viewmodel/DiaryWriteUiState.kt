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
     * 화면에 표시하고 서버로 보낼 기록일. 사용자가 날짜 행에서 고른다 (#1008).
     *
     * 서버는 2026-08-29 부터 생성·수정 양쪽에서 `date` 를 받는다 (`Afternote-BE#244`, PR #262).
     * 미래 날짜는 400(code 2101)이라 [dateError] 로 미리 막는다.
     */
    val date: LocalDate = LocalDate.now(),
    /**
     * [date] 를 **수정 요청에 실어도 되는지.** 프리필이 서버 날짜를 준 뒤이거나 사용자가 직접
     * 고른 뒤에만 true 다.
     *
     * 신규 작성은 언제나 싣는다(기본값이 오늘이고 그대로 저장되는 것이 맞다). 문제는 수정
     * 경로다 — 프리필이 실패한 채 «오늘» 을 실어 보내면 기존 기록일이 조용히 오늘로 옮겨진다.
     * 그럴 땐 키를 생략해 서버가 기존 값을 유지하게 한다 (#1008).
     */
    val isDateChosen: Boolean = false,
    /** 고를 수 없는 날짜를 짚었을 때의 안내. 서버가 400 을 주기 전에 화면에서 먼저 막는다 (#1008). */
    val dateError: UiText? = null,
    /** `GET /users/receivers` 로 불러온 내 수신인 목록. */
    val receivers: List<Receiver> = emptyList(),
    /**
     * 수신인 조회 실패 안내. **빈 목록과 구분한다** — 조용히 흡수하면 사용자는 시트를 열어
     * 빈 목록만 보고 «등록된 수신인이 없다» 로 읽는다 (#1019). 작성 자체는 계속 가능하다.
     */
    val receiverLoadError: UiText? = null,
    /**
     * 수신인 목록을 조회하는 중.
     *
     * 이게 없으면 «못 불러옴» 과 «아직 등록 안 함» 을 갈라 놓고도, 재시도를 누른 순간부터
     * 응답이 올 때까지 `receivers` 는 빈 목록이고 오류만 지워져 시트가 다시 «등록 안 함» 을
     * 고른다 — 이 PR 이 없애려던 그 혼동이 사용자 손으로 되돌아온다 (#1019 리뷰).
     */
    val isReceiverLoading: Boolean = false,
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
