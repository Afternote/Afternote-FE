package com.afternote.core.common.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * 앱 알림의 content [PendingIntent]를 동일한 Android 계약으로 생성한다.
 *
 * [PendingIntent]의 동일성 비교에는 extras가 포함되지 않는다. 따라서 같은 [requestCode]를
 * 재사용하더라도 서로 다른 알림 발생이 alias되지 않도록 action에 [source]와
 * [occurrenceToken]을 모두 포함한다. 대상 Activity 이름은 알지 않고 패키지의 런처 Intent를
 * 사용하므로 이 모듈은 app/feature Route에 의존하지 않는다.
 *
 * 런처 Activity의 manifest `launchMode`는 그대로 두고 `NEW_TASK | CLEAR_TOP | SINGLE_TOP`을
 * 이 알림 Intent에만 적용한다. 따라서 cold start와 기존 top Activity의 `onNewIntent` 진입을
 * 같은 launcher entry로 전달한다. PendingIntent는 Android 12 이상의 명시적 mutability 계약에
 * 맞춰 immutable로 만들고, 동일한 source/token 발생만 creator가 extras를 갱신할 수 있게 한다.
 *
 * @param requestCode 호출자가 관리하는 PendingIntent request code
 * @param source 알림을 만든 내부 출처
 * @param occurrenceToken 같은 출처 안에서 알림 발생을 구분하는 토큰
 * @param payload 앱 모듈이 해석할 선택적 primitive payload Bundle. 공용 모듈은 내용을 해석하지 않는다.
 */
object NotificationPendingIntentFactory {
    const val EXTRA_NOTIFICATION_ENTRY = "com.afternote.notification.ENTRY"
    const val EXTRA_NOTIFICATION_SOURCE = "com.afternote.notification.SOURCE"
    const val EXTRA_NOTIFICATION_OCCURRENCE_TOKEN = "com.afternote.notification.OCCURRENCE_TOKEN"
    const val EXTRA_NOTIFICATION_PAYLOAD = "com.afternote.notification.PAYLOAD"

    fun create(
        context: Context,
        requestCode: Int,
        source: String,
        occurrenceToken: String,
        payload: Bundle?,
    ): PendingIntent? {
        val action = notificationAction(source = source, occurrenceToken = occurrenceToken)
        val launchIntent =
            context.packageManager
                .getLaunchIntentForPackage(context.packageName)
                ?.apply {
                    payload?.let { putExtra(EXTRA_NOTIFICATION_PAYLOAD, it) }
                    putExtra(EXTRA_NOTIFICATION_ENTRY, true)
                    putExtra(EXTRA_NOTIFICATION_SOURCE, source)
                    putExtra(EXTRA_NOTIFICATION_OCCURRENCE_TOKEN, occurrenceToken)
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
            requestCode,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    internal fun notificationAction(
        source: String,
        occurrenceToken: String,
    ): String {
        require(source.isNotBlank()) { "Notification source must not be blank" }
        require(occurrenceToken.isNotBlank()) { "Notification occurrence token must not be blank" }

        // 길이 prefix로 경계를 보존해 `a` + `bc`와 `ab` + `c` 같은 조합도 같은 action이 되지 않는다.
        return "$ACTION_PREFIX:${source.length}:$source:${occurrenceToken.length}:$occurrenceToken"
    }

    private const val ACTION_PREFIX = "com.afternote.notification.OPEN"
}
