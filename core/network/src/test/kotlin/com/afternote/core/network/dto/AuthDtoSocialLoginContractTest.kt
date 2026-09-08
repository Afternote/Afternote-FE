package com.afternote.core.network.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 소셜 로그인 응답 `data.isNewUser` 수신 계약 회귀 가드 (#993).
 *
 * 이 값 하나가 신규 가입자를 온보딩(약관 동의)으로 보낼지 가른다. 그런데 키 이름이 어긋나도
 * 타입은 못 잡고 파싱도 깨지지 않는다 — 값만 비어서 전원이 조용히 "기존 유저" 가 된다.
 * 실제로 FE 가 `newUser` 로 적어 둔 2026-05-09 ~ 08-26 동안 소셜 신규 가입자가 온보딩을
 * 통째로 건너뛰었고, 아무 신호도 나지 않았다. 배포 스키마 그대로의 JSON 을 디코드해 보는
 * 이 테스트가 그 실패를 잡는 유일한 그물이다.
 *
 * 기준: 배포 서버 OpenAPI `SocialLoginResponse` — `isNewUser: boolean` (2026-08-24 실측).
 * Json 설정은 `NetworkModule.provideJson` 과 동일.
 */
class AuthDtoSocialLoginContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private fun payload(newUserField: String) =
        """{"status":200,"code":200,"message":"성공","data":{"accessToken":"a","refreshToken":"r"$newUserField}}"""

    @Test
    fun `신규 가입자 - 배포 스키마의 isNewUser true 를 디코드`() {
        val data = json.decodeFromString<BaseResponse<LoginDto.SocialLoginDto>>(payload(""","isNewUser":true""")).requireData()

        assertTrue(data.isNewUser)
        assertEquals("a", data.accessToken)
    }

    @Test
    fun `기존 유저 - isNewUser false 를 디코드`() {
        val data = json.decodeFromString<BaseResponse<LoginDto.SocialLoginDto>>(payload(""","isNewUser":false""")).requireData()

        assertFalse(data.isNewUser)
        assertEquals("r", data.refreshToken)
    }

    @Test
    fun `옛 키 newUser 만 오면 디코드 실패 — 이번 사고가 조용히 통과하지 않는다`() {
        // #993 의 실패 모양 그 자체. 기본값을 두면 여기서 false 로 흡수돼 전원이 온보딩을 건너뛴다.
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<LoginDto.SocialLoginDto>>(payload(""","newUser":true"""))
        }
    }

    @Test
    fun `isNewUser 누락 - 디코드 실패로 계약 위반이 드러난다`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<LoginDto.SocialLoginDto>>(payload(""))
        }
    }

    @Test
    fun `isNewUser 가 null 이면 흡수하지 않고 실패한다`() {
        // 전역 coerceInputValues 를 걷어냈으므로(#1494) 기본값 유무와 무관하게 null 은 실패한다.
        // 이 단언은 그 «null 을 조용히 삼키지 않는다» 를 고정한다.
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<LoginDto.SocialLoginDto>>(payload(""","isNewUser":null"""))
        }
    }

    @Test
    fun `expiresIn 은 여전히 선택 - 생략해도 isNewUser 디코드에 영향 없다`() {
        val data = json.decodeFromString<BaseResponse<LoginDto.SocialLoginDto>>(payload(""","isNewUser":true""")).requireData()

        assertTrue(data.isNewUser)
        assertEquals(null, data.expiresIn)
    }
}
