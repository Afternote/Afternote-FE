package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.account.AccountRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 설정 > 비밀번호 변경.
 *
 * 서버·네트워크·데이터·도메인 계약은 이미 있고(`AccountRepository.passwordChange` →
 * `POST auth/password/change`) 여기가 그 첫 소비처다.
 */
@HiltViewModel
class PasswordChangeViewModel
    @Inject
    constructor(
        private val accountRepository: AccountRepository,
    ) : MviViewModel<PasswordChangeIntent, PasswordChangeUiState, PasswordChangeReducerEvent>(
            PasswordChangeUiState(),
        ) {
        override fun onIntent(intent: PasswordChangeIntent) {
            when (intent) {
                is PasswordChangeIntent.UpdateCurrentPassword -> {
                    dispatch(PasswordChangeReducerEvent.CurrentPasswordChanged(intent.value))
                }

                is PasswordChangeIntent.UpdateNewPassword -> {
                    dispatch(PasswordChangeReducerEvent.NewPasswordChanged(intent.value))
                }

                PasswordChangeIntent.Submit -> {
                    submit()
                }

                PasswordChangeIntent.ConsumeChanged -> {
                    dispatch(PasswordChangeReducerEvent.ChangedConsumed)
                }
            }
        }

        override fun reduce(
            state: PasswordChangeUiState,
            event: PasswordChangeReducerEvent,
        ): PasswordChangeUiState =
            when (event) {
                // 입력이 바뀌면 직전 실패 안내는 지운다 — 고친 값에 대고 옛 사유를 읽히지 않는다.
                is PasswordChangeReducerEvent.CurrentPasswordChanged -> {
                    state.copy(currentPassword = event.value, errorMessage = null)
                }

                is PasswordChangeReducerEvent.NewPasswordChanged -> {
                    state.copy(newPassword = event.value, errorMessage = null)
                }

                PasswordChangeReducerEvent.SubmitStarted -> {
                    state.copy(isSubmitting = true, errorMessage = null)
                }

                PasswordChangeReducerEvent.Succeeded -> {
                    state.copy(isSubmitting = false, errorMessage = null, changed = PasswordChanged)
                }

                is PasswordChangeReducerEvent.Failed -> {
                    state.copy(isSubmitting = false, errorMessage = event.message)
                }

                PasswordChangeReducerEvent.ChangedConsumed -> {
                    state.copy(changed = null)
                }
            }

        /**
         * 버튼이 비활성이어도 다시 막는다 — IME 의 완료 액션처럼 버튼을 거치지 않는 제출 경로가
         * 같은 진입점으로 들어온다.
         */
        private fun submit() {
            val state = currentState
            if (!state.isSubmitEnabled) return

            dispatch(PasswordChangeReducerEvent.SubmitStarted)
            viewModelScope.launch {
                accountRepository
                    .passwordChange(
                        currentPassword = state.currentPassword,
                        newPassword = state.newPassword,
                    ).onSuccess {
                        dispatch(PasswordChangeReducerEvent.Succeeded)
                    }.onFailure { error ->
                        dispatch(PasswordChangeReducerEvent.Failed(error.toPasswordChangeMessage()))
                    }
            }
        }
    }
