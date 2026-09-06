package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingUiState {
    data object Loading : SettingUiState

    data class Success(
        val name: String,
        val email: String,
    ) : SettingUiState

    data class Error(
        val message: String,
    ) : SettingUiState
}

sealed interface WithdrawUiState {
    data object Idle : WithdrawUiState

    data object Loading : WithdrawUiState

    data object Success : WithdrawUiState

    data object Error : WithdrawUiState
}

/**
 * 설정 화면용 ViewModel.
 *
 * 로그아웃 시 [AuthRepository.logout] 한 번 호출로 끝낸다. 내부적으로 서버 호출은 best-effort,
 * 로컬 토큰은 결과 무관하게 정리하도록 위임돼 있다. 호출처는 토큰 정리 여부를 신경 쓸 필요 없음.
 */
@HiltViewModel
class SettingViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<SettingUiState>(SettingUiState.Loading)
        val uiState = _uiState.asStateFlow()

        private val _logoutCompleted = MutableStateFlow(false)
        val logoutCompleted = _logoutCompleted.asStateFlow()
        private var loadJob: Job? = null

        init {
            loadProfile()
        }

        fun refresh() = loadProfile()

        private fun loadProfile() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    _uiState.value = SettingUiState.Loading
                    runCatchingCancellable { userRepository.getMyProfile() }
                        .onSuccess { profile ->
                            _uiState.value =
                                SettingUiState.Success(
                                    name = profile.name,
                                    email = profile.email,
                                )
                        }.onFailure {
                            _uiState.value = SettingUiState.Error("프로필을 불러올 수 없습니다.")
                        }
                }
        }

        fun logout() {
            viewModelScope.launch {
                // logout() 내부에서 서버 호출(best-effort) + 로컬 토큰 정리까지 처리한다.
                authRepository.logout()
                _logoutCompleted.value = true
            }
        }

        private val _withdrawUiState = MutableStateFlow<WithdrawUiState>(WithdrawUiState.Idle)
        val withdrawUiState = _withdrawUiState.asStateFlow()

        fun deleteAccount() {
            if (_withdrawUiState.value == WithdrawUiState.Loading) return
            _withdrawUiState.value = WithdrawUiState.Loading

            viewModelScope.launch {
                try {
                    userRepository.deleteAccount()
                    _withdrawUiState.value = WithdrawUiState.Success
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    _withdrawUiState.value = WithdrawUiState.Error
                }
            }
        }

        fun dismissWithdrawError() {
            if (_withdrawUiState.value == WithdrawUiState.Error) {
                _withdrawUiState.value = WithdrawUiState.Idle
            }
        }
    }
