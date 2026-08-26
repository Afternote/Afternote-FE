package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.TodayMood
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
    val canSubmit: Boolean
        get() =
            title.isNotBlank() &&
                content.isNotBlank() &&
                mood != null &&
                submitState != SubmitState.InProgress &&
                !isDraftLoading &&
                !isUploadingImage &&
                // 프리필이 실패했는데 저장하면, 보지 못한 기존 draft 내용을 빈 폼으로 PATCH 해
                // 덮어쓴다. 이어쓰기 진입에서 프리필이 실패한 동안은 저장을 막는다 (#716).
                !(isEditingDraft && draftLoadError != null)

    val selectedReceivers: List<Receiver>
        get() = receivers.filter { it.receiverId in selectedReceiverIds }
}
