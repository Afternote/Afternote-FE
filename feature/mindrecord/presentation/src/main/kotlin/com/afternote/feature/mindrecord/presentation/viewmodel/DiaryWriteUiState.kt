package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.TodayMood
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
    val canSubmit: Boolean
        get() =
            title.isNotBlank() &&
                content.isNotBlank() &&
                mood != null &&
                submitState != SubmitState.InProgress &&
                !isDraftLoading

    val selectedReceivers: List<Receiver>
        get() = receivers.filter { it.receiverId in selectedReceiverIds }
}
