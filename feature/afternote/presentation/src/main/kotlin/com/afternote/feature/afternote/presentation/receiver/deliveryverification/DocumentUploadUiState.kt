package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.error.ReceiverServerRejectionException

/**
 * 화면이 표시할 에러 문구를 VM → UI 로 실어 나르는 상자 (payload = 운반되는 내용물).
 * sealed 로 "i18n string resource" vs "서버 동적 message" 상호 배타 보장.
 *
 * 두 경우를 각각 별도 nullable 필드로 두면 컨벤션 의존 (둘 다 set 되는 버그 가능). sealed 로 묶으면
 * 타입 자체가 "하나만 가능" 강제.
 *
 * 두 타입을 String 하나로 합칠 수 없는 이유: 리소스 ID → String 변환에는 Context 가 필요해
 * VM 에서 못 풀고(UI 의 stringResource 가 마지막에 한 번), 서버 동적 문구는 리소스가 될 수 없다.
 *
 * 쓰이는 곳이 열람 인증 흐름뿐인 건 서버 문구를 그대로 노출해도 되는 계약이 여기에만 있어서다.
 * 같은 역할의 `core.ui.UiText.DynamicOrResource` 가 뒤늦게 들어와 중복이며, 통일은 #446 소관.
 */
sealed interface ErrorPayload {
    /** 클라이언트가 미리 정의한 generic 문구 (i18n 가능). 서버 message 미제공·5xx 장애 시 fallback. */
    data class Res(
        @param:StringRes val id: Int,
    ) : ErrorPayload

    /** 백엔드가 런타임에 내려준 사용자 친화 message (예: 409 "이미 대기 중인 인증 요청이 존재합니다."). */
    data class Text(
        val message: String,
    ) : ErrorPayload
}

/**
 * status 만 가르고 code 를 안 보면 4xx 검증류 개발자 문구까지 화면에 실린다 — `@Valid` 실패의
 * "인증번호는 UUID 형식이어야 합니다."(리터럴 code=400, #600 실측)가 그 반례다.
 */
internal fun Throwable.toErrorPayload(
    @StringRes fallbackRes: Int,
): ErrorPayload =
    (this as? ReceiverServerRejectionException)
        ?.takeIf { it.isUserDisplayableRejection() }
        ?.serverMessage
        ?.takeIf { it.isNotBlank() }
        ?.let { ErrorPayload.Text(it) }
        ?: ErrorPayload.Res(fallbackRes)

/** 문구 유무는 보지 않는다 — 호출부 체인이 거른다. */
private fun ReceiverServerRejectionException.isUserDisplayableRejection(): Boolean =
    status in DISPLAYABLE_CLIENT_ERROR_RANGE && serverCode in USER_DISPLAYABLE_SERVER_CODES

/** 등재 code 라도 5xx 봉투면 장애다 — code 게이트와 독립으로 대역을 한 번 더 본다. */
private val DISPLAYABLE_CLIENT_ERROR_RANGE = 400..499

/**
 * 1900 유효하지 않은 인증번호(마스터 키) · 1901 미등록 수신자 이메일 · 1902 인증번호 만료/미존재 ·
 * 1903 인증번호 불일치 · 2008 이미 대기 중인 인증 요청. BE `ErrorCode.java`@release 전수 대조로
 * 문구가 사용자 안내인 것만 골랐다(2026-08-01 기준) — 서버가 표시 가능 여부를 알려주지는 않으므로,
 * 신규 code 는 문구를 확인한 뒤에만 등재한다. 미등재 기본값은 폴백.
 */
private val USER_DISPLAYABLE_SERVER_CODES = setOf(1900, 1901, 1902, 1903, 2008)

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
