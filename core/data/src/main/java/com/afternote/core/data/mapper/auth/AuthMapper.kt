package com.afternote.core.data.mapper.auth

import com.afternote.core.model.EmailVerifyResult
import com.afternote.core.model.LoginResult
import com.afternote.core.model.ReissueResult
import com.afternote.core.model.SignUpResult
import com.afternote.core.network.dto.LoginData
import com.afternote.core.network.dto.ReissueData
import com.afternote.core.network.dto.SignUpData
import com.afternote.core.network.dto.VerifyEmailData

/**
 * Auth DTO를 Domain 모델로 변환. (스웨거 기준)
 * TODO:리팩토링 점검 필요
 */
object AuthMapper {
    fun toEmailVerifyResult(dto: VerifyEmailData): EmailVerifyResult = EmailVerifyResult(isVerified = dto.isVerified ?: true)

    fun toSignUpResult(dto: SignUpData): SignUpResult = SignUpResult(userId = dto.userId, email = dto.email)

    fun toLoginResult(dto: LoginData?): LoginResult = LoginResult(accessToken = dto?.accessToken, refreshToken = dto?.refreshToken)

    fun toReissueResult(dto: ReissueData): ReissueResult = ReissueResult(accessToken = dto.accessToken, refreshToken = dto.refreshToken)
}
