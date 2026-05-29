package com.afternote.core.data.mapper.auth

import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.SignUpData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [AuthMapper] DTO → Domain 필드 매핑 회귀 가드.
 *
 * 매퍼가 단순 필드 복사라 분기 로직은 없지만, `accessToken` ↔ `refreshToken` 자리 바뀜·필드 누락·
 * nullable 경계(`isNewUser`) 같은 사고를 잡는다. 두 토큰엔 서로 다른 sentinel 값을 줘서 자리가
 * 바뀌면 테스트가 통과하지 못하게 한다.
 */
class AuthMapperTest {
    @Test
    fun `toSignUpResult - userId·email 매핑`() {
        val dto = SignUpData(userId = 42L, email = "user@example.com")

        val result = AuthMapper.toSignUpResult(dto)

        assertEquals(42L, result.userId)
        assertEquals("user@example.com", result.email)
    }

    @Test
    fun `toDefaultLoginResult - 토큰 매핑 (자리 바뀜 가드)`() {
        val dto =
            LoginData.DefaultLoginData(
                accessToken = "access-1",
                refreshToken = "refresh-1",
            )

        val result = AuthMapper.toDefaultLoginResult(dto)

        assertEquals("access-1", result.accessToken)
        assertEquals("refresh-1", result.refreshToken)
    }

    @Test
    fun `toSocialLoginResult - 토큰·isNewUser 매핑 (null 경계 포함)`() {
        // 서버가 newUser 를 생략할 수 있어(null) 그대로 보존돼야 한다 — true/false/null 왕복 확인.
        // 토큰 자리 바뀜도 같은 케이스에서 함께 가드.
        listOf(true, false, null).forEach { newUser ->
            val dto =
                LoginData.SocialLoginData(
                    accessToken = "access-1",
                    refreshToken = "refresh-1",
                    isNewUser = newUser,
                )

            val result = AuthMapper.toSocialLoginResult(dto)

            assertEquals("accessToken (isNewUser=$newUser)", "access-1", result.accessToken)
            assertEquals("refreshToken (isNewUser=$newUser)", "refresh-1", result.refreshToken)
            assertEquals("isNewUser 보존", newUser, result.isNewUser)
        }
    }

    @Test
    fun `toRotateTokenResult - 토큰 매핑 (자리 바뀜 가드)`() {
        val dto =
            ReissueData(
                accessToken = "access-1",
                refreshToken = "refresh-1",
            )

        val result = AuthMapper.toRotateTokenResult(dto)

        assertEquals("access-1", result.accessToken)
        assertEquals("refresh-1", result.refreshToken)
    }
}
