package com.afternote.afternote_fe.notification

import android.content.Intent
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.common.notification.NotificationPendingIntentFactory

/** 앱이 직접 생성하는 알림 진입 출처. BE FCM data key와는 별개의 내부 계약이다. */
internal enum class NotificationEntrySource(
    val contractValue: String,
) {
    DAILY("daily"),
    FCM("fcm"),
    ;

    companion object {
        private val valuesByContractValue = entries.associateBy(NotificationEntrySource::contractValue)

        fun fromValue(value: String): NotificationEntrySource? = valuesByContractValue[value]
    }
}

/**
 * 알림 탭이 앱의 cold/warm 진입 경계까지 도착했다는 내부 이벤트.
 *
 * [destination]은 화면 이동의 목적지이지 이벤트의 식별자가 아니다. 그래서 [identityKey]에는
 * 들어가지 않는다 — 같은 발생을 다시 게시하며 목적지만 바뀌어도 「이미 소비한 알림」이라는
 * 판정은 유지돼야 한다.
 */
internal data class NotificationEntryRequest(
    val source: NotificationEntrySource,
    val occurrenceId: String,
    val destination: NotificationDestination,
) {
    /** 서로 다른 source가 같은 occurrence 문자열을 사용해도 소비 상태가 충돌하지 않는 내부 키. */
    val identityKey: String =
        "${source.contractValue.length}:${source.contractValue}:" +
            "${occurrenceId.length}:$occurrenceId"
}

/** [NotificationPendingIntentFactory]가 표시한 앱 직접 생성 알림 진입만 해석한다. */
internal object NotificationIntentContract {
    fun fromIntent(intent: Intent): NotificationEntryRequest? =
        resolve(
            isNotificationEntry =
                runCatching {
                    intent.getBooleanExtra(EXTRA_NOTIFICATION_ENTRY, false)
                }.getOrDefault(false),
            rawSource =
                runCatching {
                    intent.getStringExtra(EXTRA_NOTIFICATION_SOURCE)
                }.getOrNull(),
            occurrenceId =
                runCatching {
                    intent.getStringExtra(EXTRA_NOTIFICATION_OCCURRENCE_TOKEN)
                }.getOrNull(),
            rawDestination =
                runCatching {
                    intent.getStringExtra(EXTRA_NOTIFICATION_DESTINATION)
                }.getOrNull(),
        )

    /**
     * source·occurrence는 **거부** 조건이고 목적지는 **폴백** 조건이다. 앞의 둘이 깨지면 이
     * Intent가 우리가 만든 알림인지조차 확신할 수 없어 이벤트로 승격하지 않는다. 반면 목적지는
     * 이미 확인된 알림의 부가 정보라, 계약 밖 값이 와도 탭 자체는 살리고 [FALLBACK_DESTINATION]
     * 으로 떨어뜨린다 — 알림을 눌렀는데 아무 일도 안 일어나는 편이 더 나쁘다.
     */
    internal fun resolve(
        isNotificationEntry: Boolean,
        rawSource: String?,
        occurrenceId: String?,
        rawDestination: String?,
    ): NotificationEntryRequest? {
        if (!isNotificationEntry) return null

        val source =
            rawSource
                ?.takeIf(String::isNotBlank)
                ?.let(NotificationEntrySource::fromValue)
                ?: return null
        val id = occurrenceId?.takeIf(String::isNotBlank) ?: return null

        return NotificationEntryRequest(
            source = source,
            occurrenceId = id,
            destination =
                NotificationDestination.fromContractValue(rawDestination)
                    ?: FALLBACK_DESTINATION,
        )
    }

    /** 목적지 키가 없거나 계약 밖 값일 때 여는 화면. */
    internal val FALLBACK_DESTINATION = NotificationDestination.HOME

    private const val EXTRA_NOTIFICATION_ENTRY =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY
    private const val EXTRA_NOTIFICATION_SOURCE =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE
    private const val EXTRA_NOTIFICATION_OCCURRENCE_TOKEN =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN
    private const val EXTRA_NOTIFICATION_DESTINATION =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_DESTINATION
}
