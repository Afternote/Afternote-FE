package com.afternote.afternote_fe.messaging

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.afternote.core.domain.push.PushPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 지금 이 앱의 알림이 사용자에게 보이는지 읽는다 (#2321). */
interface PushPermissionReader {
    fun current(): PushPermission
}

/**
 * `POST_NOTIFICATIONS` 런타임 권한이 아니라 [NotificationManagerCompat.areNotificationsEnabled] 를 본다.
 * API 33 미만에는 런타임 권한이 없지만 사용자가 시스템 설정에서 앱 알림을 끌 수 있고, 33 이상에서는
 * 이 값이 런타임 권한 거부까지 함께 반영한다. 서버가 알고 싶은 것은 「알림이 보이는가」 하나다.
 */
@Singleton
class SystemPushPermissionReader
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : PushPermissionReader {
        override fun current(): PushPermission =
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PushPermission.GRANTED
            } else {
                PushPermission.DENIED
            }
    }
