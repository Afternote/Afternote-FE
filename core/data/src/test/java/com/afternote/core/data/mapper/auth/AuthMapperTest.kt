package com.afternote.core.data.mapper.auth

import com.afternote.core.model.Session
import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.SignUpData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [AuthMapper] DTO→Domain 변환 회귀 가드. 토큰/식별자 필드의 누락·뒤바뀜을 막는다.
 */
class AuthMapperTest {
    @Test
    fun `toSignUpResult - userId·email 전달`() {
        val result = AuthMapper.toSignUpResult(SignUpData(userId = 42L, email = "a@b.com"))
        assertEquals(42L, result.userId)
        assertEquals("a@b.com", result.email)
    }

    @Test
    fun `toDefaultLoginResult - 토큰 전달 + DefaultSession 매핑`() {
        val result =
            AuthMapper.toDefaultLoginResult(
                LoginData.DefaultLoginData(accessToken = "at", refreshToken = "rt"),
            )
        assertEquals(Session.DefaultSession(accessToken = "at", refreshToken = "rt"), result)
    }

    @Test
    fun `toSocialLoginResult - 토큰·isNewUser 전달`() {
        val result =
            AuthMapper.toSocialLoginResult(
                LoginData.SocialLoginData(accessToken = "at", refreshToken = "rt", isNewUser = true),
            )
        assertEquals(
            Session.SocialSession(accessToken = "at", refreshToken = "rt", isNewUser = true),
            result,
        )
    }

    @Test
    fun `toSocialLoginResult - isNewUser null 보존`() {
        val result =
            AuthMapper.toSocialLoginResult(
                LoginData.SocialLoginData(accessToken = "at", refreshToken = "rt", isNewUser = null),
            )
        assertNull(result.isNewUser)
    }

    @Test
    fun `toRotateTokenResult - 토큰 전달`() {
        val result = AuthMapper.toRotateTokenResult(ReissueData(accessToken = "at", refreshToken = "rt"))
        assertEquals("at", result.accessToken)
        assertEquals("rt", result.refreshToken)
    }
}
