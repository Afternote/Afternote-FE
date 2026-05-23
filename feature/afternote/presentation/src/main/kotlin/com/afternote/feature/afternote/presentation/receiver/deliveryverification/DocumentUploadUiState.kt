package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.compose.runtime.Immutable

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
     * 클라이언트가 미리 정의한 generic 문구 (`strings.xml` 의 string resource id).
     * i18n 가능. 서버 message 가 없을 때 fallback.
     *
     * [errorMessage] 와 *상호 배타* — 둘 다 set 하지 않는다 (타입으로 강제 X, 컨벤션).
     * 화면 측 우선순위는 [errorMessage] 가 우선, 없으면 본 필드 사용.
     */
    val errorMessageRes: Int? = null,
    /**
     * 백엔드가 런타임에 내려준 사용자 친화 message 를 그대로 노출할 때 사용
     * (예: 409 "이미 대기 중인 인증 요청이 존재합니다."). i18n 불가 — 서버가 한국어로 보낸 가정.
     *
     * [errorMessageRes] 와 *상호 배타*. 화면 측은 본 필드를 우선 노출.
     *
     * (후속 정리 후보: 두 필드를 `ErrorPayload` sealed 로 합치면 상호 배타 강제 가능 — 다른 VM
     * 의 동일 패턴까지 일괄 정리될 때 도입 검토.)
     */
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() =
            !isSubmitting &&
                deathCertificate.fileUrl != null &&
                familyRelationCertificate.fileUrl != null
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

sealed interface DocumentUploadEvent {
    data object Submitted : DocumentUploadEvent
}
