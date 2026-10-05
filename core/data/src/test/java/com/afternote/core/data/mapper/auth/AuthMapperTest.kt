package com.afternote.core.data.mapper.auth

import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.EmailFindDto
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.SignUpDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Auth DTO → Domain 변환 회귀 가드.
 *
 * 매퍼가 단순 필드 복사라 분기 로직은 없지만, `accessToken` ↔ `refreshToken` 자리 바뀜·필드 누락
 * 같은 사고를 잡는다. 두 토큰엔 서로 다른 sentinel 값을 줘서 자리가 바뀌면 도메인 객체 equals
 * 비교가 실패하도록 하고, 결과는 전체 객체 단위로 비교한다.
 */
class AuthMapperTest {
    @Test
    fun `SignUpDto toDomain - userId·email 매핑`() {
        val result = SignUpDto(userId = 42L, email = "user@example.com").toDomain()

        assertEquals(AccountRegistration(userId = 42L, email = "user@example.com"), result)
    }

    @Test
    fun `EmailFindDto toDomain - name·email 매핑 (자리 바뀜 가드)`() {
        // 둘 다 String 이라 자리가 바뀌어도 컴파일은 통과한다 — 서로 구분되는 값으로 가드.
        val result = EmailFindDto(name = "박채연", email = "parkchae01@gmail.com").toDomain()

        assertEquals(FoundAccount(name = "박채연", email = "parkchae01@gmail.com"), result)
    }

    @Test
    fun `DefaultLoginDto toDomain - 토큰 매핑 (자리 바뀜 가드)`() {
        val result =
            LoginDto.DefaultLoginDto(accessToken = "access-1", refreshToken = "refresh-1").toDomain()

        assertEquals(
            Session.DefaultSession(accessToken = "access-1", refreshToken = "refresh-1"),
            result,
        )
    }

    @Test
    fun `SocialLoginDto toDomain - 토큰·isNewUser 매핑`() {
        // isNewUser 는 온보딩 진입을 가르는 값이라 뒤집히면 안 된다 — true/false 왕복 확인(#993).
        // 토큰 자리 바뀜도 같은 케이스에서 객체 비교로 함께 가드.
        listOf(true, false).forEach { newUser ->
            val result =
                LoginDto
                    .SocialLoginDto(
                        accessToken = "access-1",
                        refreshToken = "refresh-1",
                        isNewUser = newUser,
                    ).toDomain()

            assertEquals(
                "isNewUser=$newUser",
                Session.SocialSession(accessToken = "access-1", refreshToken = "refresh-1", isNewUser = newUser),
                result,
            )
        }
    }

    @Test
    fun `ReissueDto toDomain - 토큰·expiresIn 매핑 (자리 바뀜 가드)`() {
        val result =
            ReissueDto(accessToken = "access-1", refreshToken = "refresh-1", expiresIn = 3599).toDomain()

        assertEquals(
            TokenBundle(accessToken = "access-1", refreshToken = "refresh-1", expiresIn = 3599),
            result,
        )
    }
}
