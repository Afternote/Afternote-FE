package com.afternote.feature.setting.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionActionTest {
    @Test
    fun `Android 13 이상에서 권한이 없으면 런타임 권한을 요청한다`() {
        val action = notificationPermissionAction(sdkInt = 33, permissionGranted = false)

        assertEquals(NotificationPermissionAction.RequestPermission, action)
    }

    @Test
    fun `Android 12 이하에서는 시스템 알림 설정을 연다`() {
        val action = notificationPermissionAction(sdkInt = 32, permissionGranted = false)

        assertEquals(NotificationPermissionAction.OpenSettings, action)
    }

    @Test
    fun `이미 권한이 있으면 시스템 알림 설정을 연다`() {
        val action = notificationPermissionAction(sdkInt = 33, permissionGranted = true)

        assertEquals(NotificationPermissionAction.OpenSettings, action)
    }
}
