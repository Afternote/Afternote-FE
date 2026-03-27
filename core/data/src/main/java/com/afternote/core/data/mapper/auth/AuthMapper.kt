package com.afternote.core.data.mapper.auth

import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.EmailVerification
import com.afternote.core.model.SocialSession
import com.afternote.core.model.TokenBundle
import com.afternote.core.model.UserSession
import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.SignUpData
import com.afternote.core.network.dto.SocialLoginData
import com.afternote.core.network.dto.VerifyEmailData

/**
 * Auth DTO를 Domain 모델로 변환. (스웨거 기준)
 * TODO:리팩토링 점검 필요
 */
object AuthMapper {
    fun toEmailVerifyResult(dto: VerifyEmailData): EmailVerification = EmailVerification(isVerified = dto.isVerified ?: true)

    fun toSignUpResult(dto: SignUpData): AccountRegistration = AccountRegistration(userId = dto.userId, email = dto.email)

    fun toLoginResult(dto: LoginData?): UserSession = UserSession(accessToken = dto?.accessToken, refreshToken = dto?.refreshToken)

    fun toSocialLoginResult(dto: SocialLoginData?): SocialSession =
        SocialSession(
            accessToken = dto?.accessToken,
            refreshToken = dto?.refreshToken,
            isNewUser = dto?.isNewUser,
        )

    fun toRotateTokenResult(dto: ReissueData): TokenBundle = TokenBundle(accessToken = dto.accessToken, refreshToken = dto.refreshToken)
}
