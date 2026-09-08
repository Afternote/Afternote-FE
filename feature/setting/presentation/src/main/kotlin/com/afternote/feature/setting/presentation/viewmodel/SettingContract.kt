package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal data class SettingUiState(
    val profile: SettingProfileState = SettingProfileState.Loading,
    val logoutCompleted: Unit? = null,
    val withdraw: WithdrawUiState = WithdrawUiState.Idle,
) : UiState

internal sealed interface SettingProfileState {
    data object Loading : SettingProfileState

    data class Success(
        val name: String,
        val email: String,
    ) : SettingProfileState

    data class Error(
        val message: String,
    ) : SettingProfileState
}

internal sealed interface WithdrawUiState {
    data object Idle : WithdrawUiState

    data object Loading : WithdrawUiState

    data object Success : WithdrawUiState

    data object Error : WithdrawUiState
}

internal sealed interface SettingIntent : MviIntent {
    data object Refresh : SettingIntent

    data object Logout : SettingIntent

    data object ConsumeLogoutSuccess : SettingIntent

    data object DeleteAccount : SettingIntent

    data object DismissWithdrawError : SettingIntent
}

internal sealed interface SettingReducerEvent : ReducerEvent {
    data object ProfileLoading : SettingReducerEvent

    data class ProfileLoaded(
        val name: String,
        val email: String,
    ) : SettingReducerEvent

    data object ProfileFailed : SettingReducerEvent

    data object LoggedOut : SettingReducerEvent

    data object LogoutConsumed : SettingReducerEvent

    data object Withdrawing : SettingReducerEvent

    data object Withdrawn : SettingReducerEvent

    data object WithdrawFailed : SettingReducerEvent

    data object WithdrawErrorDismissed : SettingReducerEvent
}
