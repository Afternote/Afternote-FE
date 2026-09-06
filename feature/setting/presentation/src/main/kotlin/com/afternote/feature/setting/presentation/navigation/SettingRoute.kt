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
    data class RecipientListRoute(
        val selectForDeliveryConditions: Boolean = false,
    ) : SettingRoute

    @Serializable
    data object RecipientRegisterRoute : SettingRoute

    @Serializable
    data class RecipientEditRoute(
        val receiverId: Long,
    ) : SettingRoute

    @Serializable
    data class AfterDeliveryRoute(
        val receiverId: Long,
    ) : SettingRoute

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

    @Serializable
    data object InquiryListRoute : SettingRoute

    @Serializable
    data class InquiryDetailRoute(
        val inquiryId: Long,
    ) : SettingRoute

    @Serializable
    data object InquiryWriteRoute : SettingRoute
}
