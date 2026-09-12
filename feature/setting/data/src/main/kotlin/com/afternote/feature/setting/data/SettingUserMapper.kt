package com.afternote.feature.setting.data

import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.network.dto.UserConnectedAccountDto
import com.afternote.core.network.dto.UserMarketingConsentDto
import com.afternote.core.network.dto.UserPushSettingDto

internal fun UserPushSettingDto.toDomain(): UserPushSetting =
    UserPushSetting(
        timeLetter = timeLetter,
        mindRecord = mindRecord,
        afterNote = afterNote,
    )

internal fun UserMarketingConsentDto.toDomain(): UserMarketingConsent =
    UserMarketingConsent(
        sms = sms,
        email = email,
        push = push,
    )

internal fun UserConnectedAccountDto.toDomain(): UserConnectedAccount =
    UserConnectedAccount(
        local = local,
        google = google,
        naver = naver,
        kakao = kakao,
        apple = apple,
        localEmail = localEmail,
        googleEmail = googleEmail,
        naverEmail = naverEmail,
        kakaoEmail = kakaoEmail,
        appleEmail = appleEmail,
    )
