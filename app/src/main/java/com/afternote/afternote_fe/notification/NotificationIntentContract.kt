package com.afternote.afternote_fe.notification

import android.content.Intent
import com.afternote.core.common.notification.NotificationPendingIntentFactory

/** 앱 안에서 허용하는 인증 후 최상위 알림 목적지. BE FCM data key와는 별개의 내부 계약이다. */
internal enum class NotificationTopLevelDestination(
    val contractValue: String,
) {
    HOME("home"),
    MIND_RECORD("mind_record"),
    TIME_LETTER("time_letter"),
    AFTERNOTE("afternote"),
    ;

    companion object {
        private val valuesByContractValue = entries.associateBy(NotificationTopLevelDestination::contractValue)

        fun fromValue(value: String): NotificationTopLevelDestination? = valuesByContractValue[value]
    }
}

/** Bundle에서 허용하는 값의 종류를 primitive로 제한한 앱 내부 표현. */
internal sealed interface NotificationPrimitiveParameter {
    data class Text(
        val value: String,
    ) : NotificationPrimitiveParameter

    data class Flag(
        val value: Boolean,
    ) : NotificationPrimitiveParameter

    data class Integer(
        val value: Int,
    ) : NotificationPrimitiveParameter

    data class LongInteger(
        val value: Long,
    ) : NotificationPrimitiveParameter
}

internal data class NotificationNavigationRequest(
    val destination: NotificationTopLevelDestination,
    val occurrenceToken: String,
    val parameters: Map<String, NotificationPrimitiveParameter>,
)

/**
 * 런처 [Intent] 중 [NotificationPendingIntentFactory]가 표시한 알림 진입만 해석한다.
 *
 * 목적지는 안전한 최상위 route whitelist로 제한한다. 목적지가 없거나 알 수 없거나 타입이
 * 잘못된 경우 로그인 후 Home으로만 보낸다. Route 객체나 Parcelable/Serializable 객체는
 * 외부 Intent에서 복원하지 않고, payload 값도 명시한 primitive 타입만 허용한다.
 *
 * 현재 FCM 서비스는 BE 목적지 계약이 없으므로 payload를 전달하지 않는다. 이 계약은 향후
 * 서버 계약을 별도 adapter에서 명시적으로 변환할 때 사용할 앱 내부 경계다.
 */
internal object NotificationIntentContract {
    const val EXTRA_TARGET = "com.afternote.notification.TARGET"

    @Suppress("DEPRECATION")
    fun fromIntent(intent: Intent): NotificationNavigationRequest? {
        val isNotificationEntry =
            runCatching {
                intent.getBooleanExtra(EXTRA_NOTIFICATION_ENTRY, false)
            }.getOrDefault(false)
        val occurrenceToken =
            runCatching {
                intent.getStringExtra(EXTRA_NOTIFICATION_OCCURRENCE_TOKEN)
            }.getOrNull()

        return resolvePayloadSafely(
            isNotificationEntry = isNotificationEntry,
            occurrenceToken = occurrenceToken,
        ) {
            val payload = intent.getBundleExtra(EXTRA_NOTIFICATION_PAYLOAD)
            val rawTarget = payload?.get(EXTRA_TARGET)
            val rawParameters =
                payload
                    ?.run {
                        keySet()
                            .asSequence()
                            .filterNot(RESERVED_PAYLOAD_KEYS::contains)
                            .associateWith(::get)
                    }.orEmpty()

            rawTarget to rawParameters
        }
    }

    internal fun resolvePayloadSafely(
        isNotificationEntry: Boolean,
        occurrenceToken: String?,
        readPayload: () -> Pair<Any?, Map<String, Any?>>,
    ): NotificationNavigationRequest? {
        if (!isNotificationEntry) return null
        val token = occurrenceToken?.takeIf(String::isNotBlank) ?: return null

        return runCatching {
            val (rawTarget, rawParameters) = readPayload()
            resolve(
                isNotificationEntry = true,
                occurrenceToken = token,
                rawTarget = rawTarget,
                rawParameters = rawParameters,
            ) ?: homeFallback(token)
        }.getOrElse {
            homeFallback(token)
        }
    }

    internal fun resolve(
        isNotificationEntry: Boolean,
        occurrenceToken: String?,
        rawTarget: Any?,
        rawParameters: Map<String, Any?>,
    ): NotificationNavigationRequest? {
        if (!isNotificationEntry) return null
        val token = occurrenceToken?.takeIf(String::isNotBlank) ?: return null
        val destination =
            (rawTarget as? String)
                ?.let(NotificationTopLevelDestination::fromValue)
                ?: return homeFallback(token)

        val parameters =
            buildMap {
                rawParameters.forEach { (key, rawValue) ->
                    if (key.isBlank()) return homeFallback(token)
                    put(key, rawValue.toPrimitiveParameter() ?: return homeFallback(token))
                }
            }

        return NotificationNavigationRequest(
            destination = destination,
            occurrenceToken = token,
            parameters = parameters,
        )
    }

    private fun homeFallback(occurrenceToken: String): NotificationNavigationRequest =
        NotificationNavigationRequest(
            destination = NotificationTopLevelDestination.HOME,
            occurrenceToken = occurrenceToken,
            parameters = emptyMap(),
        )

    private fun Any?.toPrimitiveParameter(): NotificationPrimitiveParameter? =
        when (this) {
            is String -> NotificationPrimitiveParameter.Text(this)
            is Boolean -> NotificationPrimitiveParameter.Flag(this)
            is Int -> NotificationPrimitiveParameter.Integer(this)
            is Long -> NotificationPrimitiveParameter.LongInteger(this)
            else -> null
        }

    private val RESERVED_PAYLOAD_KEYS = setOf(EXTRA_TARGET)

    private const val EXTRA_NOTIFICATION_ENTRY =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY
    private const val EXTRA_NOTIFICATION_OCCURRENCE_TOKEN =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN
    private const val EXTRA_NOTIFICATION_PAYLOAD =
        NotificationPendingIntentFactory.EXTRA_NOTIFICATION_PAYLOAD
}
