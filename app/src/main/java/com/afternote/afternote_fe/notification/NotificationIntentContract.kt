package com.afternote.afternote_fe.notification

import android.content.Intent
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
 * 실제 화면 목적지는 #1111의 BE payload 계약이 확정된 뒤 별도 adapter에서 추가한다. 이 단계에서는
 * 출처와 occurrence만 보존해 Activity 수명주기와 PendingIntent identity를 목적지 계약과 분리한다.
 */
internal data class NotificationEntryRequest(
    val source: NotificationEntrySource,
    val occurrenceId: String,
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
        )

    /**
     * Intent 에서 꺼낸 원시 값만 받아 판정한다. `Intent` 를 직접 읽지 않는 덕에 «무엇을 거부하는가»
     * 가 Android API 접근과 섞이지 않는다. 밖에서는 [fromIntent] 하나로만 들어온다.
     */
    private fun resolve(
        isNotificationEntry: Boolean,
        rawSource: String?,
        occurrenceId: String?,
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
        )
    }

    private const val EXTRA_NOTIFICATION_ENTRY =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY
    private const val EXTRA_NOTIFICATION_SOURCE =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE
    private const val EXTRA_NOTIFICATION_OCCURRENCE_TOKEN =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN
}
