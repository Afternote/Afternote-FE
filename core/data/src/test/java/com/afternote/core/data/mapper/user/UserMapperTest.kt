package com.afternote.core.data.mapper.user

import com.afternote.core.model.user.DeliveryConditionType
import com.afternote.core.network.dto.DeliveryConditionTypeDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [UserMapper] enum 매핑 회귀 가드.
 * 서버 enum 추가/누락 시 컴파일은 통과해도 매핑이 비어 새 케이스가 도메인에 누락되는 사고를 막는다.
 * (정의: `DeliveryConditionTypeDto` ↔ `DeliveryConditionType`)
 */
class UserMapperTest {
    @Test
    fun `DeliveryConditionTypeDto toDomain - 전체 케이스 매핑`() {
        // enum 전체 순회 — 새 DTO 케이스 추가 시 when 분기 누락이 컴파일에서 실패.
        // expected 산출: DTO 상수의 선언 이름(`dto.name`, 예: "NONE") 으로 Domain enum 의 동일
        // 이름 상수를 `valueOf` 조회. UserMapper.toDomain() 이 `when` 분기로 매핑한 결과가
        // 동일 이름끼리 짝지어지는지 검증 — 두 구현이 독립이라 자기검증이 아님.
        DeliveryConditionTypeDto.entries.forEach { dto ->
            val expected = DeliveryConditionType.valueOf(dto.name)
            assertEquals("DTO $dto → Domain 매핑", expected, dto.toDomain())
        }
    }

    @Test
    fun `DeliveryConditionType toDto - 전체 케이스 매핑`() {
        // 역방향. 위와 동일 패턴 — Domain 상수 이름으로 DTO enum 의 동일 이름 상수 조회 후
        // mapper 결과와 비교.
        DeliveryConditionType.entries.forEach { domain ->
            val expected = DeliveryConditionTypeDto.valueOf(domain.name)
            assertEquals("Domain $domain → DTO 매핑", expected, domain.toDto())
        }
    }

    @Test
    fun `DeliveryConditionType 매핑은 양방향 round-trip 보존`() {
        DeliveryConditionType.entries.forEach { domain ->
            assertEquals(domain, domain.toDto().toDomain())
        }
        DeliveryConditionTypeDto.entries.forEach { dto ->
            assertEquals(dto, dto.toDomain().toDto())
        }
    }

    @Test
    fun `DeliveryConditionType domain - DTO enum 개수 동기화`() {
        // 한 쪽에만 case 가 추가되면 round-trip 으로는 못 잡는 경우가 있어 명시적 카운트 확인.
        assertEquals(DeliveryConditionTypeDto.entries.size, DeliveryConditionType.entries.size)
    }
}
