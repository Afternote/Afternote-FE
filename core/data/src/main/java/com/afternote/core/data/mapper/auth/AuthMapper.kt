package com.afternote.core.data.mapper.auth

import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.SignUpData

/**
 * Auth DTO를 Domain 모델로 변환. (스웨거 기준)
 */
object AuthMapper {
    fun toSignUpResult(dto: SignUpData): AccountRegistration = AccountRegistration(userId = dto.userId, email = dto.email)

    fun toDefaultLoginResult(dto: LoginData.DefaultLoginData): Session.DefaultSession =
        Session.DefaultSession(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
        )

    fun toSocialLoginResult(dto: LoginData.SocialLoginData): Session.SocialSession =
        Session.SocialSession(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            isNewUser = dto.isNewUser,
        )

    fun toRotateTokenResult(dto: ReissueData): TokenBundle =
        TokenBundle(accessToken = dto.accessToken, refreshToken = dto.refreshToken, expiresIn = dto.expiresIn)
}
