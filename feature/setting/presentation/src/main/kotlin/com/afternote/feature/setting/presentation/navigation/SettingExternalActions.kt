package com.afternote.feature.setting.presentation.navigation

/** Auth changes belong to the app shell; each clears the authenticated root stack. */
public interface SettingExternalActions {
    public fun onLogoutSuccess()

    public fun onWithdrawSuccess()
}
