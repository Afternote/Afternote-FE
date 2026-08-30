package com.afternote.core.data.mapper.auth

import com.afternote.core.model.PasskeyAuthenticationOptions
import com.afternote.core.network.dto.PasskeyAuthenticateRequestDto
import com.afternote.core.network.dto.PasskeyAuthenticationOptionsDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * 패스키 wire DTO ↔ Credential Manager 가 먹는 JSON 원문 사이의 변환.
 *
 * 봉투(`BaseResponse`)를 벗기고 나면 서버 응답은 이미 WebAuthn 표준 옵션의 모양이지만,
 * Credential Manager 가 받는 것은 **객체가 아니라 JSON 문자열**이라 한 번 더 직렬화해야 한다.
 * 그 조립을 여기 모아 두어 단위 테스트로 못박는다 — 키 이름이 하나만 어긋나도 시스템 선택기가
 * 뜨지 않는데, 그 실패는 실기기에서만 드러나기 때문이다.
 */
object PasskeyMapper {
    fun toAuthenticationOptions(dto: PasskeyAuthenticationOptionsDto): PasskeyAuthenticationOptions =
        PasskeyAuthenticationOptions(
            requestJson =
                buildJsonObject {
                    put("challenge", dto.challenge)
                    put("timeout", dto.timeout)
                    put("rpId", dto.rpId)
                    putJsonArray("allowCredentials") {
                        dto.allowCredentials.forEach { descriptor ->
                            addJsonObject {
                                put("type", descriptor.type)
                                put("id", descriptor.id)
                            }
                        }
                    }
                    put("userVerification", dto.userVerification)
                }.toString(),
        )

    /**
     * assertion 원문을 **트리로만 옮겨** 요청 본문에 싣는다.
     *
     * 필드를 골라 담지 않는 이유 — 서명 대상(`clientDataJSON`)과 서명값이 이 안에 들어 있어
     * 앱이 재조립하면 서버 검증이 깨진다. 파싱은 형식 확인 이상을 하지 않는다.
     *
     * @throws kotlinx.serialization.SerializationException assertion 이 JSON 이 아닐 때.
     * @throws IllegalArgumentException assertion 이 JSON 객체가 아닐 때.
     */
    fun toAuthenticateRequest(assertionJson: String): PasskeyAuthenticateRequestDto =
        PasskeyAuthenticateRequestDto(credential = assertionJson.toJsonObject())

    private fun String.toJsonObject(): JsonObject = Json.parseToJsonElement(this).jsonObject
}
