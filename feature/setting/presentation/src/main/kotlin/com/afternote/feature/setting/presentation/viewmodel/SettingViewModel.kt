package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SettingViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
    ) : MviViewModel<SettingIntent, SettingUiState, SettingReducerEvent>(SettingUiState()) {
        private var loadJob: Job? = null
        private var logoutJob: Job? = null

        init {
            loadProfile()
        }

        override fun onIntent(intent: SettingIntent) {
            when (intent) {
                SettingIntent.Refresh -> loadProfile()
                SettingIntent.Logout -> logout()
                SettingIntent.ConsumeLogoutSuccess -> dispatch(SettingReducerEvent.LogoutConsumed)
                SettingIntent.DeleteAccount -> deleteAccount()
                SettingIntent.DismissWithdrawError -> dispatch(SettingReducerEvent.WithdrawErrorDismissed)
            }
        }

        override fun reduce(
            state: SettingUiState,
            event: SettingReducerEvent,
        ): SettingUiState =
            when (event) {
                SettingReducerEvent.ProfileLoading -> {
                    if (state.profile is SettingProfileState.Success) state else state.copy(profile = SettingProfileState.Loading)
                }

                is SettingReducerEvent.ProfileLoaded -> {
                    state.copy(profile = SettingProfileState.Success(event.name, event.email))
                }

                SettingReducerEvent.ProfileFailed -> {
                    state.copy(profile = SettingProfileState.Error("프로필을 불러올 수 없습니다."))
                }

                SettingReducerEvent.LoggedOut -> {
                    state.copy(logoutCompleted = Unit)
                }

                SettingReducerEvent.LogoutConsumed -> {
                    state.copy(logoutCompleted = null)
                }

                SettingReducerEvent.Withdrawing -> {
                    state.copy(withdraw = WithdrawUiState.Loading)
                }

                SettingReducerEvent.Withdrawn -> {
                    state.copy(withdraw = WithdrawUiState.Success)
                }

                SettingReducerEvent.WithdrawFailed -> {
                    state.copy(withdraw = WithdrawUiState.Error)
                }

                SettingReducerEvent.WithdrawErrorDismissed -> {
                    if (state.withdraw ==
                        WithdrawUiState.Error
                    ) {
                        state.copy(withdraw = WithdrawUiState.Idle)
                    } else {
                        state
                    }
                }
            }

        private fun loadProfile() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    dispatch(SettingReducerEvent.ProfileLoading)
                    runCatchingCancellable { userRepository.getMyProfile() }
                        .onSuccess { dispatch(SettingReducerEvent.ProfileLoaded(it.name, it.email)) }
                        .onFailure { dispatch(SettingReducerEvent.ProfileFailed) }
                }
        }

        private fun logout() {
            if (logoutJob?.isActive == true || currentState.logoutCompleted != null) return
            logoutJob =
                viewModelScope.launch {
                    authRepository.logout()
                    dispatch(SettingReducerEvent.LoggedOut)
                }
        }

        private fun deleteAccount() {
            if (currentState.withdraw == WithdrawUiState.Loading || currentState.withdraw == WithdrawUiState.Success) return
            dispatch(SettingReducerEvent.Withdrawing)
            viewModelScope.launch {
                runCatchingCancellable { userRepository.deleteAccount() }
                    .onSuccess { dispatch(SettingReducerEvent.Withdrawn) }
                    .onFailure { dispatch(SettingReducerEvent.WithdrawFailed) }
            }
        }
    }
