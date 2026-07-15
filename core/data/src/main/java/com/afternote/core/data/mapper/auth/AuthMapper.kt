package com.afternote.core.data.mapper.auth

import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.SignUpDto

/**
 * Auth DTO를 Domain 모델로 변환. (스웨거 기준)
 */
object AuthMapper {
    fun toSignUpResult(dto: SignUpDto): AccountRegistration = AccountRegistration(userId = dto.userId, email = dto.email)

    fun toDefaultLoginResult(dto: LoginDto.DefaultLoginDto): Session.DefaultSession =
        Session.DefaultSession(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
        )

    fun toSocialLoginResult(dto: LoginDto.SocialLoginDto): Session.SocialSession =
        Session.SocialSession(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            isNewUser = dto.isNewUser,
        )

    fun toRotateTokenResult(dto: ReissueDto): TokenBundle =
        TokenBundle(accessToken = dto.accessToken, refreshToken = dto.refreshToken, expiresIn = dto.expiresIn)
}
