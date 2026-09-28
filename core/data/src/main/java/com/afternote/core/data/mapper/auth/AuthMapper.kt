package com.afternote.core.data.mapper.auth

import com.afternote.core.model.AccountRegistration
import com.afternote.core.model.FoundAccount
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.EmailFindDto
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.ReissueDto
import com.afternote.core.network.dto.SignUpDto

/*
 * Auth DTO를 Domain 모델로 변환. (스웨거 기준)
 */

internal fun SignUpDto.toDomain(): AccountRegistration = AccountRegistration(userId = userId, email = email)

internal fun EmailFindDto.toDomain(): FoundAccount = FoundAccount(name = name, email = email)

internal fun LoginDto.DefaultLoginDto.toDomain(): Session.DefaultSession =
    Session.DefaultSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
    )

internal fun LoginDto.SocialLoginDto.toDomain(): Session.SocialSession =
    Session.SocialSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        isNewUser = isNewUser,
    )

internal fun ReissueDto.toDomain(): TokenBundle = TokenBundle(accessToken = accessToken, refreshToken = refreshToken, expiresIn = expiresIn)
