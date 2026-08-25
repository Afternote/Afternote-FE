package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.afternote.feature.receiver.domain.error.ReceiverFailure

/**
 * 화면이 표시할 에러 문구를 VM → UI 로 실어 나른다. 리소스와 서버 동적 문구를 각각 nullable 필드로
 * 두면 둘 다 set 되는 상태가 가능해지므로, sealed 로 "하나만" 을 타입에 강제한다.
 *
 * 둘을 String 하나로 합칠 수는 없다 — 리소스 ID → String 변환에 Context 가 필요해 VM 에서 못 풀고
 * (UI 의 stringResource 가 마지막에 한 번), 서버 동적 문구는 리소스가 될 수 없다.
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

/** 실패를 화면 문구로 옮긴다 — 노출 가능 여부 판정은 [isUserDisplayableRejection] 이 가른다. */
internal fun Throwable.toErrorPayload(
    @StringRes fallbackRes: Int,
): ErrorPayload =
    (this as? ReceiverFailure)
        ?.displayTextOrNull()
        ?.let { ErrorPayload.Text(it) }
        ?: ErrorPayload.Res(fallbackRes)

/**
 * 화면에 그대로 실어도 되는 서버 문구. 루트로 좁혀 `when` 을 exhaustive 하게 만든다 — 수신자 실패
 * 유형이 늘면 여기가 컴파일 에러로 잡힌다. `else` 로 뭉개 두면 새 유형이 게이트를 거치지 않은 채
 * 흘러가 노출 규약이 조용히 빈다.
 *
 * null 은 "노출할 서버 문구 없음" 이고, 폴백 리소스는 [toErrorPayload] 한 곳에서만 적용한다.
 */
private fun ReceiverFailure.displayTextOrNull(): String? =
    when (this) {
        is ReceiverFailure.ServerRejection -> {
            serverMessage?.takeIf { isUserDisplayableRejection() && it.isNotBlank() }
        }
    }

/**
 * status 만 보고 code 를 안 보면 4xx 검증류 개발자 문구까지 화면에 실린다 — `@Valid` 실패가
 * "인증번호는 UUID 형식이어야 합니다." 를 리터럴 code=400 으로 실어 보낸 것(#600 실측)이 그 반례다.
 * 문구 유무는 여기서 보지 않는다 — 호출부 체인이 거른다.
 */
private fun ReceiverFailure.ServerRejection.isUserDisplayableRejection(): Boolean =
    status in DISPLAYABLE_CLIENT_ERROR_RANGE && serverCode in USER_DISPLAYABLE_SERVER_CODES

/** 등재 code 라도 5xx 봉투면 장애다 — code 게이트와 독립으로 대역을 한 번 더 본다. */
private val DISPLAYABLE_CLIENT_ERROR_RANGE = 400..499

/**
 * 서버는 표시 가능 여부를 알려주지 않는다 — BE `ErrorCode` 의 실제 문구가 사용자 안내인 것만 골라
 * 등재한 목록이다. 신규 code 는 문구를 확인한 뒤에만 더한다. 미등재 기본값은 폴백.
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
