package com.afternote.feature.receiver.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `POST /receiver-auth/delivery-verification` 요청 계약 회귀 가드 (#380).
 *
 * 서버가 두 서류 URL 을 모두 요구하던 스펙을 "하나 이상" 으로 완화 — 2026-07-07 라이브 Swagger
 * (`afternote.kro.kr/v3/api-docs`) 실측: `DeliveryVerificationRequestDto` 스키마에서 required 제거,
 * 두 필드 모두 nullable, description "두 서류 중 하나 이상 필수".
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일 (ignoreUnknownKeys).
 * `encodeDefaults` 미설정(기본 false) + DTO 의 `= null` default 조합이라, 제출하지 않은 슬롯은
 * 페이로드에서 **키 자체가 생략**된다 — null 을 명시 전송하지 않아도 서버가 미제출로 해석하는
 * 형태를 이 테스트가 고정한다.
 */
class DeliveryVerificationContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `사망진단서만 제출 - 미제출 슬롯 키는 페이로드에서 생략`() {
        val body = DeliveryVerificationRequestDto(deathCertificateUrl = "https://bucket/death.pdf")

        val encoded = json.encodeToString(DeliveryVerificationRequestDto.serializer(), body)

        assertEquals("""{"deathCertificateUrl":"https://bucket/death.pdf"}""", encoded)
    }

    @Test
    fun `가족관계증명서만 제출 - 미제출 슬롯 키는 페이로드에서 생략`() {
        val body = DeliveryVerificationRequestDto(familyRelationCertificateUrl = "https://bucket/family.pdf")

        val encoded = json.encodeToString(DeliveryVerificationRequestDto.serializer(), body)

        assertEquals("""{"familyRelationCertificateUrl":"https://bucket/family.pdf"}""", encoded)
    }

    @Test
    fun `두 서류 모두 제출 - 두 키 모두 포함`() {
        val body =
            DeliveryVerificationRequestDto(
                deathCertificateUrl = "https://bucket/death.pdf",
                familyRelationCertificateUrl = "https://bucket/family.pdf",
            )

        val encoded = json.encodeToString(DeliveryVerificationRequestDto.serializer(), body)

        assertEquals(
            """{"deathCertificateUrl":"https://bucket/death.pdf","familyRelationCertificateUrl":"https://bucket/family.pdf"}""",
            encoded,
        )
    }
}
