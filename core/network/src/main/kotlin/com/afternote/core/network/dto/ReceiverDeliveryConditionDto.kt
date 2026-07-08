package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 사후 전달(사후 관리) — 수신자 × 콘텐츠 단위 전달 조건.
 *
 * 스웨거:
 * - GET  /api/v1/users/me/receivers/{receiverId}/delivery-conditions
 * - PUT  /api/v1/users/me/receivers/{receiverId}/delivery-conditions
 *
 * 구 `users/delivery-condition`(유저 단위, 단일 조건)을 대체한다.
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
enum class DeliveryConditionTriggerDto {
    // 미사용 자동 전달
    @SerialName("INACTIVITY")
    INACTIVITY,

    // 수신자 요청(서류 승인)
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
enum class DeliveryConditionStateDto {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("PENDING_CONFIRMATION")
    PENDING_CONFIRMATION,

    @SerialName("WAITING_VERIFICATION")
    WAITING_VERIFICATION,

    @SerialName("FULFILLED")
    FULFILLED,
}

@Serializable
data class DeliveryConditionItemRequest(
    @SerialName("contentType") val contentType: DeliveryContentTypeDto,
    @SerialName("conditionType") val conditionType: DeliveryConditionTriggerDto,
    @SerialName("inactivityPeriod") val inactivityPeriod: InactivityPeriodDto? = null,
)

@Serializable
data class ReceiverDeliveryConditionUpdateRequest(
    @SerialName("conditions") val conditions: List<DeliveryConditionItemRequest> = emptyList(),
)

@Serializable
data class DeliveryConditionItemResponseDto(
    @SerialName("contentType") val contentType: DeliveryContentTypeDto,
    @SerialName("conditionType") val conditionType: DeliveryConditionTriggerDto,
    @SerialName("inactivityPeriod") val inactivityPeriod: InactivityPeriodDto? = null,
    @SerialName("state") val state: DeliveryConditionStateDto? = null,
    @SerialName("fulfilled") val fulfilled: Boolean = false,
    @SerialName("gracePeriodStartedAt") val gracePeriodStartedAt: String? = null,
    @SerialName("fulfilledAt") val fulfilledAt: String? = null,
)

@Serializable
data class ReceiverDeliveryConditionResponseDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("conditions") val conditions: List<DeliveryConditionItemResponseDto> = emptyList(),
)
