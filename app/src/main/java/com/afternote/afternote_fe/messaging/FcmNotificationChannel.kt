package com.afternote.afternote_fe.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.afternote.afternote_fe.R

internal object FcmNotificationChannel {
    fun create(context: Context) {
        val channel =
            NotificationChannel(
                context.getString(R.string.fcm_notification_channel_id),
                context.getString(R.string.fcm_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
