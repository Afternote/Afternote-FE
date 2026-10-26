package com.afternote.feature.setting.presentation.notification

internal enum class NotificationPermissionAction {
    RequestPermission,
    OpenSettings,
}

internal fun notificationPermissionAction(
    sdkInt: Int,
    permissionGranted: Boolean,
): NotificationPermissionAction =
    if (sdkInt >= 33 && !permissionGranted) {
        NotificationPermissionAction.RequestPermission
    } else {
        NotificationPermissionAction.OpenSettings
    }
