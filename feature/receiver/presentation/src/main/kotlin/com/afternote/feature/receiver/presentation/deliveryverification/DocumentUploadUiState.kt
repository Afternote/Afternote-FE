package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.runtime.Immutable
import com.afternote.core.ui.UiText

/**
 * 증빙 서류 업로드(6·7·8) UI 상태.
 *
 * 두 슬롯(사망진단서 + 가족관계증명서) 의 첨부 진행 상황 + 신청 제출 상태 + 입력 검증 에러를 묶는다.
 * UI 는 슬롯별 첨부 결과 [DocumentSlotState] 를 보고 빈/업로드 중/완료 표시를 분기한다.
 */
@Immutable
data class DocumentUploadUiState(
    val deathCertificate: DocumentSlotState = DocumentSlotState(),
    val familyRelationCertificate: DocumentSlotState = DocumentSlotState(),
    val isSubmitting: Boolean = false,
    /**
     * **표시 대기 중인** 에러. `null` 은 «실패가 없었다» 가 아니라 «지금 띄울 것이 없다» 다 — 화면이
     * 한 번 표시한 뒤 `consumeError()` 로 되돌리므로, 실패한 적 없는 상태와 이미 보여 준 상태가 같은
     * 값이 된다. 마지막 시도의 성패를 알아야 하면 [isSubmitting]·[isSubmitted]·슬롯의 `fileUrl` 를 본다.
     *
     * 표시 가능한 서버 message 와 클라이언트 fallback 을 [UiText] 하나로 운반하며, [isSubmitted] 와 같은 소비형 필드다.
     */
    val error: UiText? = null,
    /** 제출 성공 신호 — UI 가 LaunchedEffect 로 완료 화면 이동 후 [DocumentUploadViewModel.onSubmittedConsumed] 로 reset. */
    val isSubmitted: Boolean = false,
) {
    /**
     * 두 서류 중 하나 이상 업로드되면 제출 가능 — 서버도 최소 1개만 요구한다 (이슈 #380).
     * 단 어느 슬롯이든 업로드 진행 중이면 잠근다 — 이미 성공한 URL 만 실려 진행 중 파일이
     * 신청에서 조용히 빠지는 것 방지 (#711).
     */
    val canSubmit: Boolean
        get() =
            !isSubmitting &&
                !deathCertificate.isUploading &&
                !familyRelationCertificate.isUploading &&
                (deathCertificate.fileUrl != null || familyRelationCertificate.fileUrl != null)
}

/**
 * 단일 서류 슬롯의 상태.
 *
 * @property displayName 첨부 완료 시 노출할 파일 이름 (없으면 placeholder).
 * @property fileUrl 업로드 성공 후 받은 공개 URL. 신청 제출 페이로드에 그대로 전달된다.
 * @property isUploading presigned URL 요청 + S3 PUT 진행 중 여부.
 */
@Immutable
data class DocumentSlotState(
    val displayName: String? = null,
    val fileUrl: String? = null,
    val isUploading: Boolean = false,
)

enum class DocumentSlot {
    DeathCertificate,
    FamilyRelationCertificate,
}
