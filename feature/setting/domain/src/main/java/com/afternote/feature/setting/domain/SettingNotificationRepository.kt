package com.afternote.feature.setting.domain

import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting

public interface SettingNotificationRepository {
    // 푸시 알림 설정 조회
    public suspend fun getMyPushSettings(): UserPushSetting

    // 푸시 알림 설정 수정
    public suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting

    // 마케팅 수신 동의 조회 (문자·이메일·푸시)
    public suspend fun getMyMarketingConsents(): UserMarketingConsent

    // 마케팅 수신 동의 수정
    public suspend fun updateMyMarketingConsents(
        sms: Boolean?,
        email: Boolean?,
        push: Boolean?,
    ): UserMarketingConsent
}
