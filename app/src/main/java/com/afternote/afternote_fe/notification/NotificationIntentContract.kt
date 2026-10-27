package com.afternote.afternote_fe.notification

import android.content.Intent
import android.os.Bundle
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import java.util.UUID

/** 앱이 직접 생성하는 알림 진입 출처. BE FCM data key와는 별개의 내부 계약이다. */
internal enum class NotificationEntrySource(
    val contractValue: String,
) {
    /** 앱이 직접 울리던 데일리 알림(#2321 에서 제거). 이미 트레이에 떠 있는 알림을 위해 남겨 둔다. */
    DAILY("daily"),
    FCM("fcm"),

    /** 앱 안 알림함에서 누른 알림. Intent 를 거치지 않고 큐에 바로 들어간다. */
    INBOX("inbox"),
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
 *
 * [serverNotificationId]가 있으면 서버가 만든 알림이다. [destination]·[serverData]는 발송 시점의
 * 목적지라 탭한 시점의 목적지는 이 ID로 서버에 다시 묻는다 (#2321). 묻지 못하면 [serverData]로
 * 안쪽 화면까지, 그것도 없으면 [destination] 탭으로 연다.
 */
internal data class NotificationEntryRequest(
    val source: NotificationEntrySource,
    val occurrenceId: String,
    val destination: NotificationDestination,
    val serverNotificationId: Long? = null,
    val serverData: Map<String, String> = emptyMap(),
) {
    /** 서로 다른 source가 같은 occurrence 문자열을 사용해도 소비 상태가 충돌하지 않는 내부 키. */
    val identityKey: String =
        "${source.contractValue.length}:${source.contractValue}:" +
            "${occurrenceId.length}:$occurrenceId"

    companion object {
        /**
         * 알림함에서 누른 알림. 같은 알림을 두 번 눌러도 두 번 이동해야 하므로 누를 때마다 새 발생으로 만든다 —
         * 푸시 탭은 같은 Intent 가 다시 오는 것을 막아야 하지만, 알림함은 사용자가 매번 새로 누른다.
         */
        fun fromInbox(
            notificationId: Long,
            data: Map<String, String>,
        ): NotificationEntryRequest =
            NotificationEntryRequest(
                source = NotificationEntrySource.INBOX,
                occurrenceId = "notification:$notificationId:${UUID.randomUUID()}",
                destination = ServerNotificationDestination.resolve(data).tab,
                serverNotificationId = notificationId,
                serverData = data.filterKeys(NotificationIntentContract.SERVER_DATA_KEYS::contains),
            )
    }
}

/**
 * 알림 진입을 처리한 결과 — 열 화면과, 대상이 지워졌을 때의 안내.
 *
 * @property serverMessage 서버가 정한 안내 문구. [deleted]인데 이 값이 없으면 앱 기본 문구를 쓴다.
 */
internal data class NotificationEntryResolution(
    val navigation: NotificationNavigation,
    val deleted: Boolean = false,
    val serverMessage: String? = null,
)

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
            serverNotificationId =
                runCatching {
                    intent.getLongExtra(EXTRA_NOTIFICATION_SERVER_ID, 0L)
                }.getOrNull()
                    ?.takeIf { it > 0L },
            serverData =
                runCatching {
                    intent.getBundleExtra(EXTRA_NOTIFICATION_SERVER_DATA)?.toServerData()
                }.getOrNull()
                    .orEmpty(),
        )

    /**
     * source·occurrence는 **거부** 조건이고 목적지는 **폴백** 조건이다. 앞의 둘이 깨지면 이
     * Intent가 우리가 만든 알림인지조차 확신할 수 없어 이벤트로 승격하지 않는다. 반면 목적지는
     * 이미 확인된 알림의 부가 정보라, 계약 밖 값이 와도 탭 자체는 살리고 [FALLBACK_DESTINATION]
     * 으로 떨어뜨린다 — 알림을 눌렀는데 아무 일도 안 일어나는 편이 더 나쁘다.
     */
    private fun resolve(
        isNotificationEntry: Boolean,
        rawSource: String?,
        occurrenceId: String?,
        rawDestination: String?,
        serverNotificationId: Long?,
        serverData: Map<String, String>,
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
            serverNotificationId = serverNotificationId,
            serverData = serverData,
        )
    }

    /** 목적지 해석에 쓰는 키만 문자열로 남긴다. 알림 Intent 는 앱 밖에서도 만들 수 있는 입력이다. */
    private fun Bundle.toServerData(): Map<String, String> =
        SERVER_DATA_KEYS
            .mapNotNull { key -> getString(key)?.let { value -> key to value } }
            .toMap()

    /** [ServerNotificationDestination] 이 읽는 키. */
    val SERVER_DATA_KEYS =
        setOf("destination", "action", "recordId", "recordType", "questionId", "timeLetterId", "receiverId", "invitationId")

    /** 목적지 키가 없거나 계약 밖 값일 때 여는 화면. */
    private val FALLBACK_DESTINATION = NotificationDestination.HOME

    private const val EXTRA_NOTIFICATION_ENTRY =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY
    private const val EXTRA_NOTIFICATION_SOURCE =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE
    private const val EXTRA_NOTIFICATION_OCCURRENCE_TOKEN =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN
    private const val EXTRA_NOTIFICATION_DESTINATION =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_DESTINATION
    private const val EXTRA_NOTIFICATION_SERVER_ID =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SERVER_ID
    private const val EXTRA_NOTIFICATION_SERVER_DATA =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SERVER_DATA
}
