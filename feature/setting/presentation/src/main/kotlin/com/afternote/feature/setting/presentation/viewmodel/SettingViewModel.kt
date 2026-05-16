package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

/**
 * 설정 화면용 ViewModel.
 *
 * 서버 logout 호출은 best-effort, 로컬 토큰 [AuthRepository.clearSession]은 실패 여부와 무관하게 실행한다.
 * 서버 logout이 실패(토큰 없음, 네트워크 에러 등)하더라도 로컬 세션은 반드시 삭제한다.
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

        init {
            loadProfile()
        }

        private fun loadProfile() {
            viewModelScope.launch {
                runCatching { userRepository.getMyProfile() }
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
                runCatching { authRepository.logout() }
                authRepository.clearSession()
                userRepository.clearCachedProfile()
                _logoutCompleted.value = true
            }
        }

        private val _withdrawCompleted = MutableStateFlow(false)
        val withdrawCompleted = _withdrawCompleted.asStateFlow()

        fun deleteAccount() {
            viewModelScope.launch {
                runCatching { userRepository.deleteAccount() }
                    .onSuccess { _withdrawCompleted.value = true }
            }
        }
    }
