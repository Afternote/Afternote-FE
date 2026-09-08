package com.afternote.core.data.mapper.delivery

import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.network.dto.delivery.ConditionStateDto
import com.afternote.core.network.dto.delivery.DeliveryConditionItemDto
import com.afternote.core.network.dto.delivery.DeliveryConditionTypeDto
import com.afternote.core.network.dto.delivery.DeliveryContentTypeDto
import com.afternote.core.network.dto.delivery.InactivityPeriodDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `DeliveryConditionMapper` enum·응답 매핑 회귀 가드 (이슈 #427).
 *
 * 서버 enum 추가/누락 시 `when` 분기 누락이 도메인에 조용히 반영되는 사고를 막는다.
 * 검증 방식은 DTO/Domain 의 동일 이름 상수를 `valueOf` 로 독립 조회해 매퍼 결과와 대조하는 것이다(자기검증 아님).
 *
 * 파일 내부 helper 는 `private` 이라 매퍼의 **공개 진입점 두 개**로만 검증한다 (#1672) —
 * 응답은 [ReceiverDeliveryConditionDto.toDomain], 요청은 [DeliveryConditionItem.toRequestDto].
 * enum 한 짝을 진입점에 태워 넣고 반대편에서 읽어 내는 방식이라 분기 누락은 그대로 드러난다.
 */
class DeliveryConditionMapperTest {
    @Test
    fun `DeliveryContentType 매핑 - 양방향 전체 케이스 + round-trip + 개수 동기화`() {
        DeliveryContentTypeDto.entries.forEach { dto ->
            assertEquals(DeliveryContentType.valueOf(dto.name), mapResponse(contentType = dto).contentType)
        }
        DeliveryContentType.entries.forEach { domain ->
            val requested = mapRequest(contentType = domain).contentType
            assertEquals(DeliveryContentTypeDto.valueOf(domain.name), requested)
            assertEquals(domain, mapResponse(contentType = requested).contentType)
        }
        assertEquals(DeliveryContentTypeDto.entries.size, DeliveryContentType.entries.size)
    }

    @Test
    fun `DeliveryConditionType 매핑 - 양방향 전체 케이스 + round-trip + 개수 동기화`() {
        DeliveryConditionTypeDto.entries.forEach { dto ->
            assertEquals(DeliveryConditionType.valueOf(dto.name), mapResponse(conditionType = dto).conditionType)
        }
        DeliveryConditionType.entries.forEach { domain ->
            val requested = mapRequest(conditionType = domain).conditionType
            assertEquals(DeliveryConditionTypeDto.valueOf(domain.name), requested)
            assertEquals(domain, mapResponse(conditionType = requested).conditionType)
        }
        assertEquals(DeliveryConditionTypeDto.entries.size, DeliveryConditionType.entries.size)
    }

    @Test
    fun `InactivityPeriod 매핑 - 양방향 전체 케이스 + round-trip + 개수 동기화`() {
        InactivityPeriodDto.entries.forEach { dto ->
            assertEquals(InactivityPeriod.valueOf(dto.name), mapResponse(inactivityPeriod = dto).inactivityPeriod)
        }
        InactivityPeriod.entries.forEach { domain ->
            val requested = mapRequest(inactivityPeriod = domain).inactivityPeriod
            assertEquals(InactivityPeriodDto.valueOf(domain.name), requested)
            assertEquals(domain, mapResponse(inactivityPeriod = requested).inactivityPeriod)
        }
        assertEquals(InactivityPeriodDto.entries.size, InactivityPeriod.entries.size)
    }

    @Test
    fun `ConditionState 매핑 - 응답 전용 전체 케이스 + 개수 동기화`() {
        // ConditionState 는 서버 판정 값이라 응답 방향만 존재 — 요청 DTO 에는 필드 자체가 없다.
        ConditionStateDto.entries.forEach { dto ->
            assertEquals(ConditionState.valueOf(dto.name), mapResponse(state = dto).state)
        }
        assertEquals(ConditionStateDto.entries.size, ConditionState.entries.size)
    }

    @Test
    fun `응답 매핑 - receiverId 와 항목 필드를 보존한다`() {
        val response =
            ReceiverDeliveryConditionDto(
                receiverId = 42L,
                conditions =
                    listOf(
                        itemDto(
                            contentType = DeliveryContentTypeDto.AFTERNOTE,
                            conditionType = DeliveryConditionTypeDto.INACTIVITY,
                            inactivityPeriod = InactivityPeriodDto.ONE_YEAR,
                            state = ConditionStateDto.PENDING_CONFIRMATION,
                        ),
                    ),
            )

        val domain = response.toDomain()

        assertEquals(42L, domain.receiverId)
        val item = domain.conditions.single()
        assertEquals(DeliveryContentType.AFTERNOTE, item.contentType)
        assertEquals(DeliveryConditionType.INACTIVITY, item.conditionType)
        assertEquals(InactivityPeriod.ONE_YEAR, item.inactivityPeriod)
        assertEquals(ConditionState.PENDING_CONFIRMATION, item.state)
        assertEquals(false, item.fulfilled)
        assertEquals("2026-07-08T00:00:00Z", item.gracePeriodStartedAt)
        assertEquals(null, item.fulfilledAt)
    }

    @Test
    fun `요청 매핑 - RECEIVER_REQUEST 는 기간 null 유지`() {
        val request =
            mapRequest(
                contentType = DeliveryContentType.DIARY,
                conditionType = DeliveryConditionType.RECEIVER_REQUEST,
                inactivityPeriod = null,
            )

        assertEquals(DeliveryContentTypeDto.DIARY, request.contentType)
        assertEquals(DeliveryConditionTypeDto.RECEIVER_REQUEST, request.conditionType)
        assertEquals(null, request.inactivityPeriod)
    }

    private fun itemDto(
        contentType: DeliveryContentTypeDto = DeliveryContentTypeDto.AFTERNOTE,
        conditionType: DeliveryConditionTypeDto = DeliveryConditionTypeDto.INACTIVITY,
        inactivityPeriod: InactivityPeriodDto? = InactivityPeriodDto.THREE_MONTHS,
        state: ConditionStateDto = ConditionStateDto.ACTIVE,
    ) = DeliveryConditionItemDto(
        contentType = contentType,
        conditionType = conditionType,
        inactivityPeriod = inactivityPeriod,
        state = state,
        fulfilled = false,
        gracePeriodStartedAt = "2026-07-08T00:00:00Z",
        fulfilledAt = null,
    )

    /** 응답 진입점([ReceiverDeliveryConditionDto.toDomain])으로 항목 하나를 태워 도메인으로 받는다. */
    private fun mapResponse(
        contentType: DeliveryContentTypeDto = DeliveryContentTypeDto.AFTERNOTE,
        conditionType: DeliveryConditionTypeDto = DeliveryConditionTypeDto.INACTIVITY,
        inactivityPeriod: InactivityPeriodDto? = InactivityPeriodDto.THREE_MONTHS,
        state: ConditionStateDto = ConditionStateDto.ACTIVE,
    ): DeliveryConditionItem =
        ReceiverDeliveryConditionDto(
            receiverId = 1L,
            conditions = listOf(itemDto(contentType, conditionType, inactivityPeriod, state)),
        ).toDomain()
            .conditions
            .single()

    /** 요청 진입점([DeliveryConditionItem.toRequestDto])으로 도메인 항목 하나를 DTO 로 받는다. */
    private fun mapRequest(
        contentType: DeliveryContentType = DeliveryContentType.AFTERNOTE,
        conditionType: DeliveryConditionType = DeliveryConditionType.INACTIVITY,
        inactivityPeriod: InactivityPeriod? = InactivityPeriod.THREE_MONTHS,
    ) = DeliveryConditionItem(
        contentType = contentType,
        conditionType = conditionType,
        inactivityPeriod = inactivityPeriod,
        state = ConditionState.ACTIVE,
        fulfilled = false,
        gracePeriodStartedAt = null,
        fulfilledAt = null,
    ).toRequestDto()
}
