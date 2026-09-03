package com.afternote.core.data.mapper.delivery

import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.network.dto.delivery.ConditionStateDto
import com.afternote.core.network.dto.delivery.DeliveryConditionItemDto
import com.afternote.core.network.dto.delivery.DeliveryConditionItemRequestDto
import com.afternote.core.network.dto.delivery.DeliveryConditionTypeDto
import com.afternote.core.network.dto.delivery.DeliveryContentTypeDto
import com.afternote.core.network.dto.delivery.InactivityPeriodDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto

// ========================================
// Enum Mapper (DTO → Domain)
// ========================================

private fun DeliveryContentTypeDto.toDomain(): DeliveryContentType =
    when (this) {
        DeliveryContentTypeDto.TIME_LETTER -> DeliveryContentType.TIME_LETTER
        DeliveryContentTypeDto.AFTERNOTE -> DeliveryContentType.AFTERNOTE
        DeliveryContentTypeDto.DAILY_QUESTION -> DeliveryContentType.DAILY_QUESTION
        DeliveryContentTypeDto.DIARY -> DeliveryContentType.DIARY
        DeliveryContentTypeDto.DEEP_THOUGHT -> DeliveryContentType.DEEP_THOUGHT
    }

private fun DeliveryConditionTypeDto.toDomain(): DeliveryConditionType =
    when (this) {
        DeliveryConditionTypeDto.INACTIVITY -> DeliveryConditionType.INACTIVITY
        DeliveryConditionTypeDto.RECEIVER_REQUEST -> DeliveryConditionType.RECEIVER_REQUEST
    }

private fun InactivityPeriodDto.toDomain(): InactivityPeriod =
    when (this) {
        InactivityPeriodDto.THREE_MONTHS -> InactivityPeriod.THREE_MONTHS
        InactivityPeriodDto.SIX_MONTHS -> InactivityPeriod.SIX_MONTHS
        InactivityPeriodDto.ONE_YEAR -> InactivityPeriod.ONE_YEAR
    }

private fun ConditionStateDto.toDomain(): ConditionState =
    when (this) {
        ConditionStateDto.ACTIVE -> ConditionState.ACTIVE
        ConditionStateDto.PENDING_CONFIRMATION -> ConditionState.PENDING_CONFIRMATION
        ConditionStateDto.WAITING_VERIFICATION -> ConditionState.WAITING_VERIFICATION
        ConditionStateDto.FULFILLED -> ConditionState.FULFILLED
    }

// ========================================
// Enum Mapper (Domain → DTO, 요청용)
// ========================================

private fun DeliveryContentType.toDto(): DeliveryContentTypeDto =
    when (this) {
        DeliveryContentType.TIME_LETTER -> DeliveryContentTypeDto.TIME_LETTER
        DeliveryContentType.AFTERNOTE -> DeliveryContentTypeDto.AFTERNOTE
        DeliveryContentType.DAILY_QUESTION -> DeliveryContentTypeDto.DAILY_QUESTION
        DeliveryContentType.DIARY -> DeliveryContentTypeDto.DIARY
        DeliveryContentType.DEEP_THOUGHT -> DeliveryContentTypeDto.DEEP_THOUGHT
    }

private fun DeliveryConditionType.toDto(): DeliveryConditionTypeDto =
    when (this) {
        DeliveryConditionType.INACTIVITY -> DeliveryConditionTypeDto.INACTIVITY
        DeliveryConditionType.RECEIVER_REQUEST -> DeliveryConditionTypeDto.RECEIVER_REQUEST
    }

private fun InactivityPeriod.toDto(): InactivityPeriodDto =
    when (this) {
        InactivityPeriod.THREE_MONTHS -> InactivityPeriodDto.THREE_MONTHS
        InactivityPeriod.SIX_MONTHS -> InactivityPeriodDto.SIX_MONTHS
        InactivityPeriod.ONE_YEAR -> InactivityPeriodDto.ONE_YEAR
    }

// ========================================
// Response Mapper (DTO → Domain)
// ========================================

private fun DeliveryConditionItemDto.toDomain(): DeliveryConditionItem =
    DeliveryConditionItem(
        contentType = contentType.toDomain(),
        conditionType = conditionType.toDomain(),
        inactivityPeriod = inactivityPeriod?.toDomain(),
        state = state.toDomain(),
        fulfilled = fulfilled,
        gracePeriodStartedAt = gracePeriodStartedAt,
        fulfilledAt = fulfilledAt,
    )

fun ReceiverDeliveryConditionDto.toDomain(): ReceiverDeliveryConditions =
    ReceiverDeliveryConditions(
        receiverId = receiverId,
        conditions = conditions.map { it.toDomain() },
    )

// ========================================
// Request Mapper (Domain → DTO)
// ========================================

/** 설정(PUT) 요청 항목으로 변환 — 서버 판정 필드(state/fulfilled/시각)는 보내지 않는다. */
fun DeliveryConditionItem.toRequestDto(): DeliveryConditionItemRequestDto =
    DeliveryConditionItemRequestDto(
        contentType = contentType.toDto(),
        conditionType = conditionType.toDto(),
        inactivityPeriod = inactivityPeriod?.toDto(),
    )
