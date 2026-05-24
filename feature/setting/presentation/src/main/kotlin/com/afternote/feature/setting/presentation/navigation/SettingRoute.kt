package com.afternote.feature.setting.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface SettingRoute {
    @Serializable
    data object SettingHomeRoute : SettingRoute

    @Serializable
    data object WithdrawGuideRoute : SettingRoute

    @Serializable
    data object WithdrawConfirmRoute : SettingRoute

    @Serializable
    data object ProfileEditRoute : SettingRoute

    @Serializable
    data object LinkedAccountRoute : SettingRoute

    @Serializable
    data object NotificationRoute : SettingRoute

    @Serializable
    data object RecipientListRoute : SettingRoute

    @Serializable
    data object RecipientRegisterRoute : SettingRoute

    @Serializable
    data object AfterDeliveryRoute : SettingRoute

    @Serializable
    data object PasskeyRoute : SettingRoute

    @Serializable
    data object PasskeyMakingRoute : SettingRoute

    @Serializable
    data object AppLockSetupRoute : SettingRoute

    @Serializable
    data object PasskeyPasswordRoute : SettingRoute

    @Serializable
    data object NoticeRoute : SettingRoute
}
