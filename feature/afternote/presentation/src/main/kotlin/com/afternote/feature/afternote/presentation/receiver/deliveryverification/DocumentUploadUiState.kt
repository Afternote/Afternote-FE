package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * UI 에 노출할 에러 — sealed 로 "i18n string resource" vs "서버 동적 message" 상호 배타 보장.
 *
 * 두 경우를 각각 별도 nullable 필드로 두면 컨벤션 의존 (둘 다 set 되는 버그 가능). sealed 로 묶으면
 * 타입 자체가 "하나만 가능" 강제.
 */
sealed interface ErrorPayload {
    /** 클라이언트가 미리 정의한 generic 문구 (i18n 가능). 서버 message 미제공 시 fallback. */
    data class Res(
        @param:StringRes val id: Int,
    ) : ErrorPayload

    /** 백엔드가 런타임에 내려준 사용자 친화 message (예: 409 "이미 대기 중인 인증 요청이 존재합니다."). */
    data class Text(
        val message: String,
    ) : ErrorPayload
}

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
    /** 표시할 에러 — null 이면 에러 없음. [ErrorPayload.Res] 또는 [ErrorPayload.Text] 둘 중 하나. */
    val error: ErrorPayload? = null,
    /** 제출 성공 신호 — UI 가 LaunchedEffect 로 완료 화면 이동 후 [DocumentUploadViewModel.onSubmittedConsumed] 로 reset. */
    val isSubmitted: Boolean = false,
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
