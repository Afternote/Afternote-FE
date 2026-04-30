package com.afternote.feature.setting.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface SettingRoute {
    @Serializable
    data object SettingHomeRoute : SettingRoute

    @Serializable
    data object WithdrawGuideRoute : SettingRoute

    @Serializable
    data object WithdrawConfirmRoute : SettingRoute
}
