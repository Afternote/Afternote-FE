package com.afternote.core.data.mapper.auth

import com.afternote.core.model.EmailVerify
import com.afternote.core.model.Login
import com.afternote.core.model.RotateToken
import com.afternote.core.model.SignUp
import com.afternote.core.model.SocialLogin
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
    fun toEmailVerifyResult(dto: VerifyEmailData): EmailVerify = EmailVerify(isVerified = dto.isVerified ?: true)

    fun toSignUpResult(dto: SignUpData): SignUp = SignUp(userId = dto.userId, email = dto.email)

    fun toLoginResult(dto: LoginData?): Login = Login(accessToken = dto?.accessToken, refreshToken = dto?.refreshToken)

    fun toSocialLoginResult(dto: SocialLoginData?): SocialLogin =
        SocialLogin(
            accessToken = dto?.accessToken,
            refreshToken = dto?.refreshToken,
            isNewUser = dto?.isNewUser,
        )

    fun toRotateTokenResult(dto: ReissueData): RotateToken = RotateToken(accessToken = dto.accessToken, refreshToken = dto.refreshToken)
}
