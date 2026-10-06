package com.afternote.feature.setting.domain

import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting

/**
 * 설정의 알림 계약 (#1429).
 *
 * 푸시 토글과 마케팅 수신 동의는 설정 밖에 소비자가 없어 `core:domain` 의 사용자 합본이 아니라 이
 * 모듈이 소유한다. `null` 인자는 그 항목을 건드리지 않는다는 뜻이고, 서버가 돌려준 전체 설정이
 * 반환값이다.
 *
 * [updateMyPushSettings] 의 실패는 구현이 `PushSettingFailure` 로 분류해 올린다 — 화면이 네트워크와
 * 서버 오류 안내를 나눠 보여줄 수 있어야 하기 때문이다.
 */
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
