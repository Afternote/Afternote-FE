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
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DeliveryConditionMapper] enum·응답 매핑 회귀 가드 (이슈 #427).
 *
 * 서버 enum 추가/누락 시 `when` 분기 누락이 도메인에 조용히 반영되는 사고를 막는다.
 * 검증 방식은 DTO/Domain 의 동일 이름 상수를 `valueOf` 로 독립 조회해 매퍼 결과와 대조하는 것이다(자기검증 아님).
 */
class DeliveryConditionMapperTest {
    @Test
    fun `DeliveryContentType 매핑 - 양방향 전체 케이스 + round-trip + 개수 동기화`() {
        DeliveryContentTypeDto.entries.forEach { dto ->
            assertEquals(DeliveryContentType.valueOf(dto.name), dto.toDomain())
        }
        DeliveryContentType.entries.forEach { domain ->
            assertEquals(DeliveryContentTypeDto.valueOf(domain.name), domain.toDto())
            assertEquals(domain, domain.toDto().toDomain())
        }
        assertEquals(DeliveryContentTypeDto.entries.size, DeliveryContentType.entries.size)
    }

    @Test
    fun `DeliveryConditionType 매핑 - 양방향 전체 케이스 + round-trip + 개수 동기화`() {
        DeliveryConditionTypeDto.entries.forEach { dto ->
            assertEquals(DeliveryConditionType.valueOf(dto.name), dto.toDomain())
        }
        DeliveryConditionType.entries.forEach { domain ->
            assertEquals(DeliveryConditionTypeDto.valueOf(domain.name), domain.toDto())
            assertEquals(domain, domain.toDto().toDomain())
        }
        assertEquals(DeliveryConditionTypeDto.entries.size, DeliveryConditionType.entries.size)
    }

    @Test
    fun `InactivityPeriod 매핑 - 양방향 전체 케이스 + round-trip + 개수 동기화`() {
        InactivityPeriodDto.entries.forEach { dto ->
            assertEquals(InactivityPeriod.valueOf(dto.name), dto.toDomain())
        }
        InactivityPeriod.entries.forEach { domain ->
            assertEquals(InactivityPeriodDto.valueOf(domain.name), domain.toDto())
            assertEquals(domain, domain.toDto().toDomain())
        }
        assertEquals(InactivityPeriodDto.entries.size, InactivityPeriod.entries.size)
    }

    @Test
    fun `ConditionState 매핑 - 응답 전용 전체 케이스 + 개수 동기화`() {
        // ConditionState 는 서버 판정 값이라 응답(toDomain)만 존재 — 요청 방향(toDto) 없음.
        ConditionStateDto.entries.forEach { dto ->
            assertEquals(ConditionState.valueOf(dto.name), dto.toDomain())
        }
        assertEquals(ConditionStateDto.entries.size, ConditionState.entries.size)
    }

    @Test
    fun `DeliveryConditionItemDto toDomain - 필드 보존 + INACTIVITY 기간 매핑`() {
        val dto =
            DeliveryConditionItemDto(
                contentType = DeliveryContentTypeDto.AFTERNOTE,
                conditionType = DeliveryConditionTypeDto.INACTIVITY,
                inactivityPeriod = InactivityPeriodDto.ONE_YEAR,
                state = ConditionStateDto.PENDING_CONFIRMATION,
                fulfilled = false,
                gracePeriodStartedAt = "2026-07-08T00:00:00Z",
                fulfilledAt = null,
            )

        val domain = dto.toDomain()

        assertEquals(DeliveryContentType.AFTERNOTE, domain.contentType)
        assertEquals(DeliveryConditionType.INACTIVITY, domain.conditionType)
        assertEquals(InactivityPeriod.ONE_YEAR, domain.inactivityPeriod)
        assertEquals(ConditionState.PENDING_CONFIRMATION, domain.state)
        assertEquals(false, domain.fulfilled)
        assertEquals("2026-07-08T00:00:00Z", domain.gracePeriodStartedAt)
        assertEquals(null, domain.fulfilledAt)
    }

    @Test
    fun `DeliveryConditionItem toRequestDto - RECEIVER_REQUEST 는 기간 null 유지`() {
        val item =
            DeliveryConditionItem(
                contentType = DeliveryContentType.DIARY,
                conditionType = DeliveryConditionType.RECEIVER_REQUEST,
                inactivityPeriod = null,
                state = ConditionState.ACTIVE,
                fulfilled = false,
                gracePeriodStartedAt = null,
                fulfilledAt = null,
            )

        val req = item.toRequestDto()

        assertEquals(DeliveryContentTypeDto.DIARY, req.contentType)
        assertEquals(DeliveryConditionTypeDto.RECEIVER_REQUEST, req.conditionType)
        assertEquals(null, req.inactivityPeriod)
    }
}
