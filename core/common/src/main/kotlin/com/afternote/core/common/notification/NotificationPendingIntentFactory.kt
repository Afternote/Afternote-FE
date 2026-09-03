package com.afternote.core.common.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * 앱 알림의 content [PendingIntent]를 동일한 Android 계약으로 생성한다.
 *
 * [PendingIntent]의 동일성 비교에는 extras가 포함되지 않는다. 따라서 내부의 고정 request code를
 * 재사용하더라도 서로 다른 알림 발생이 alias되지 않도록 action에 [source]와
 * [occurrenceId]을 모두 포함한다. 대상 Activity 이름은 알지 않고 패키지의 런처 Intent를
 * 사용하므로 이 모듈은 app/feature Route에 의존하지 않는다. [destination]도 같은 이유로
 * `Route`가 아니라 [NotificationDestination] 계약값으로 실린다 — 화면으로의 번역은 app 몫이다.
 *
 * [destination]은 action에 넣지 않는다. 목적지는 발생(occurrence)의 **속성**이지 식별자가
 * 아니므로, 같은 발생을 다시 게시하면 `FLAG_UPDATE_CURRENT`가 extras를 갱신하는 것이 옳다.
 *
 * 런처 Activity의 manifest `launchMode`는 그대로 두고 `NEW_TASK | CLEAR_TOP | SINGLE_TOP`을
 * 이 알림 Intent에만 적용한다. 따라서 cold start와 기존 top Activity의 `onNewIntent` 진입을
 * 같은 launcher entry로 전달한다. PendingIntent는 Android 12 이상의 명시적 mutability 계약에
 * 맞춰 immutable로 만들고, 동일한 source/occurrence 발생만 creator가 extras를 갱신할 수 있게 한다.
 *
 * @param source 알림을 만든 내부 출처
 * @param occurrenceId 같은 출처 안에서 알림 발생을 구분하는 식별자
 * @param destination 탭했을 때 열 최상위 화면. 생산자가 반드시 정한다 — 기본값을 두면 목적지
 *   배선을 빠뜨린 알림이 조용히 홈으로만 떨어진다.
 */
object NotificationPendingIntentFactory {
    const val EXTRA_NOTIFICATION_ENTRY = "com.afternote.notification.ENTRY"
    const val EXTRA_NOTIFICATION_SOURCE = "com.afternote.notification.SOURCE"

    /**
     * 이름과 값 모두 옛 어휘(`TOKEN`)를 유지한다 — 이건 코드 이름이 아니라 **Intent extra 키**다.
     * 앱 업데이트 전에 게시된 알림의 PendingIntent 가 이 키를 달고 살아 있어서, 값을 바꾸면 그
     * 알림을 탭했을 때 진입 정보를 못 읽는다. 상수 이름은 그 키를 그대로 가리키게 둔다.
     */
    const val EXTRA_NOTIFICATION_OCCURRENCE_TOKEN = "com.afternote.notification.OCCURRENCE_TOKEN"
    const val EXTRA_NOTIFICATION_DESTINATION = "com.afternote.notification.DESTINATION"

    fun create(
        context: Context,
        source: String,
        occurrenceId: String,
        destination: NotificationDestination,
    ): PendingIntent? {
        val action = notificationAction(source = source, occurrenceId = occurrenceId)
        val launchIntent =
            context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    putExtra(EXTRA_NOTIFICATION_ENTRY, true)
                    putExtra(EXTRA_NOTIFICATION_SOURCE, source)
                    putExtra(EXTRA_NOTIFICATION_OCCURRENCE_TOKEN, occurrenceId)
                    putExtra(EXTRA_NOTIFICATION_DESTINATION, destination.contractValue)
                    this.action = action
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                }
                ?: return null

        return PendingIntent.getActivity(
            context,
            CONTENT_INTENT_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    internal fun notificationAction(
        source: String,
        occurrenceId: String,
    ): String {
        require(source.isNotBlank()) { "Notification source must not be blank" }
        require(occurrenceId.isNotBlank()) { "Notification occurrence id must not be blank" }

        // 길이 prefix로 경계를 보존해 `a` + `bc`와 `ab` + `c` 같은 조합도 같은 action이 되지 않는다.
        return "$ACTION_PREFIX:${source.length}:$source:${occurrenceId.length}:$occurrenceId"
    }

    private const val ACTION_PREFIX = "com.afternote.notification.OPEN"
    private const val CONTENT_INTENT_REQUEST_CODE = 0
}
