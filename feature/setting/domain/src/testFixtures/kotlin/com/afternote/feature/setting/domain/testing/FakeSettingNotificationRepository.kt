package com.afternote.feature.setting.domain.testing

import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.SettingNotificationRepository
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [SettingNotificationRepository] fake 정본 (#1429).
 *
 * 푸시 토글과 마케팅 동의 상태만 메모리에 담는다. 기본 동작은 `null` 이 아닌 인자만 덮어써 서버의
 * 부분 갱신 의미를 그대로 흉내 내고, 실패 분류나 매달린 응답은 `onX` 로 갈아끼운다.
 *
 * 호출 기록 타입 [PushUpdateCall]·[MarketingConsentUpdateCall] 은 이 fake 가 소유한다.
 */
class FakeSettingNotificationRepository(
    @Volatile var pushSetting: UserPushSetting = DEFAULT_PUSH_SETTING,
    @Volatile var marketingConsent: UserMarketingConsent = DEFAULT_MARKETING_CONSENT,
    var onGetMyPushSettings: (suspend () -> UserPushSetting)? = null,
    var onUpdateMyPushSettings: (suspend (Boolean?, Boolean?, Boolean?) -> UserPushSetting)? = null,
    var onGetMyMarketingConsents: (suspend () -> UserMarketingConsent)? = null,
    var onUpdateMyMarketingConsents: (suspend (Boolean?, Boolean?, Boolean?) -> UserMarketingConsent)? = null,
) : SettingNotificationRepository {
    private val getPushSettingsCounter = AtomicInteger()
    private val getMarketingConsentsCounter = AtomicInteger()

    val pushUpdateCalls = CopyOnWriteArrayList<PushUpdateCall>()
    val marketingConsentUpdateCalls = CopyOnWriteArrayList<MarketingConsentUpdateCall>()

    val getMyPushSettingsCalls: Int get() = getPushSettingsCounter.get()
    val getMyMarketingConsentsCalls: Int get() = getMarketingConsentsCounter.get()
    val pushSettingUpdates: List<Triple<Boolean?, Boolean?, Boolean?>>
        get() = pushUpdateCalls.map { Triple(it.timeLetter, it.mindRecord, it.afterNote) }
    val marketingConsentUpdates: List<Triple<Boolean?, Boolean?, Boolean?>>
        get() = marketingConsentUpdateCalls.map { Triple(it.sms, it.email, it.push) }

    override suspend fun getMyPushSettings(): UserPushSetting {
        getPushSettingsCounter.incrementAndGet()
        onGetMyPushSettings?.let { return it() }
        return pushSetting
    }

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting {
        pushUpdateCalls += PushUpdateCall(timeLetter, mindRecord, afterNote)
        onUpdateMyPushSettings?.let { return it(timeLetter, mindRecord, afterNote) }
        pushSetting =
            UserPushSetting(
                timeLetter = timeLetter ?: pushSetting.timeLetter,
                mindRecord = mindRecord ?: pushSetting.mindRecord,
                afterNote = afterNote ?: pushSetting.afterNote,
            )
        return pushSetting
    }

    override suspend fun getMyMarketingConsents(): UserMarketingConsent {
        getMarketingConsentsCounter.incrementAndGet()
        onGetMyMarketingConsents?.let { return it() }
        return marketingConsent
    }

    override suspend fun updateMyMarketingConsents(
        sms: Boolean?,
        email: Boolean?,
        push: Boolean?,
    ): UserMarketingConsent {
        marketingConsentUpdateCalls += MarketingConsentUpdateCall(sms, email, push)
        onUpdateMyMarketingConsents?.let { return it(sms, email, push) }
        marketingConsent =
            UserMarketingConsent(
                sms = sms ?: marketingConsent.sms,
                email = email ?: marketingConsent.email,
                push = push ?: marketingConsent.push,
            )
        return marketingConsent
    }

    data class PushUpdateCall(
        val timeLetter: Boolean?,
        val mindRecord: Boolean?,
        val afterNote: Boolean?,
    )

    data class MarketingConsentUpdateCall(
        val sms: Boolean?,
        val email: Boolean?,
        val push: Boolean?,
    )

    companion object {
        internal val DEFAULT_PUSH_SETTING = UserPushSetting(true, true, true)
        internal val DEFAULT_MARKETING_CONSENT = UserMarketingConsent(true, true, false)

        fun strict(): FakeSettingNotificationRepository =
            FakeSettingNotificationRepository(
                onGetMyPushSettings = { unexpectedCall("SettingNotificationRepository.getMyPushSettings") },
                onUpdateMyPushSettings = { _, _, _ ->
                    unexpectedCall("SettingNotificationRepository.updateMyPushSettings")
                },
                onGetMyMarketingConsents = { unexpectedCall("SettingNotificationRepository.getMyMarketingConsents") },
                onUpdateMyMarketingConsents = { _, _, _ ->
                    unexpectedCall("SettingNotificationRepository.updateMyMarketingConsents")
                },
            )
    }
}
