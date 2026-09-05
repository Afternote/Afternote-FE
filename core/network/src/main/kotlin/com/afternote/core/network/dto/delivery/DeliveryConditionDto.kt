package com.afternote.core.network.dto.delivery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET/PUT /users/me/receivers/{receiverId}/delivery-conditions` DTO (이슈 #427).
 *
 * Swagger 실측(2026-07-08, afternote.kro.kr/v3/api-docs) 스키마와 1:1. 유저단위(구)
 * `DeliveryConditionTypeDto` 와 값이 달라 별도 패키지에 두었다 — 그 구 타입은 이슈 #428 에서 제거됐다.
 */

@Serializable
enum class DeliveryContentTypeDto {
    @SerialName("TIME_LETTER")
    TIME_LETTER,

    @SerialName("AFTERNOTE")
    AFTERNOTE,

    @SerialName("DAILY_QUESTION")
    DAILY_QUESTION,

    @SerialName("DIARY")
    DIARY,

    @SerialName("DEEP_THOUGHT")
    DEEP_THOUGHT,
}

@Serializable
enum class DeliveryConditionTypeDto {
    @SerialName("INACTIVITY")
    INACTIVITY,

    @SerialName("RECEIVER_REQUEST")
    RECEIVER_REQUEST,
}

@Serializable
enum class InactivityPeriodDto {
    @SerialName("THREE_MONTHS")
    THREE_MONTHS,

    @SerialName("SIX_MONTHS")
    SIX_MONTHS,

    @SerialName("ONE_YEAR")
    ONE_YEAR,
}

@Serializable
enum class ConditionStateDto {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("PENDING_CONFIRMATION")
    PENDING_CONFIRMATION,

    @SerialName("WAITING_VERIFICATION")
    WAITING_VERIFICATION,

    @SerialName("FULFILLED")
    FULFILLED,
}

// ========================================
// Request (PUT)
// ========================================

@Serializable
data class DeliveryConditionItemRequestDto(
    @SerialName("contentType") val contentType: DeliveryContentTypeDto,
    @SerialName("conditionType") val conditionType: DeliveryConditionTypeDto,
    @SerialName("inactivityPeriod") val inactivityPeriod: InactivityPeriodDto? = null,
)

@Serializable
data class ReceiverDeliveryConditionUpdateRequestDto(
    @SerialName("conditions") val conditions: List<DeliveryConditionItemRequestDto>,
)

// ========================================
// Response (GET/PUT)
// ========================================

/**
 * 수신자별 전달조건 1건의 조회 응답. 요청([DeliveryConditionItemRequestDto])엔 없는 4개 필드
 * (state·fulfilled·gracePeriodStartedAt·fulfilledAt)는 **서버가 판정해 내려주는 값**이라 응답에만 있다.
 *
 * @property state 조건 진행 상태 — ACTIVE(대기) / PENDING_CONFIRMATION(미사용 감지 후 본인확인 유예 중) /
 *   WAITING_VERIFICATION(수신자 요청 서류의 운영자 승인 대기) / FULFILLED(충족 완료). 값 정의는 [ConditionStateDto].
 * @property fulfilled 충족(열람 가능) 여부. 열람 잠금/열림 판정은 이 boolean 하나로 충분하고, state 는
 *   "왜 아직 안 열렸나(유예 중/승인 대기)" 진행 단계를 보여준다 — 결과(fulfilled) vs 과정(state) 로 역할이 갈린다.
 *   대체로 state==FULFILLED 와 동치로 보이나, "조건 변경 시 서류/검증 유지" 규칙에서 state 가 리셋돼도
 *   fulfilled 가 남을 여지가 있어 **완전 파생인지는 실서버 미확인** — 항상 동치로 단정하지 말 것.
 * @property gracePeriodStartedAt INACTIVITY 경로에서 미사용 감지 후 본인확인 유예가 시작된 시각(ISO-8601).
 *   이 시점부터 유예 카운트다운. 유예 진입 전이면 null.
 * @property fulfilledAt 조건이 충족(전달 확정)된 시각(ISO-8601). 아직 미충족이면 null.
 */
@Serializable
data class DeliveryConditionItemDto(
    @SerialName("contentType") val contentType: DeliveryContentTypeDto,
    @SerialName("conditionType") val conditionType: DeliveryConditionTypeDto,
    @SerialName("inactivityPeriod") val inactivityPeriod: InactivityPeriodDto? = null,
    @SerialName("state") val state: ConditionStateDto,
    @SerialName("fulfilled") val fulfilled: Boolean,
    @SerialName("gracePeriodStartedAt") val gracePeriodStartedAt: String? = null,
    @SerialName("fulfilledAt") val fulfilledAt: String? = null,
)

@Serializable
data class ReceiverDeliveryConditionDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("conditions") val conditions: List<DeliveryConditionItemDto>,
)
