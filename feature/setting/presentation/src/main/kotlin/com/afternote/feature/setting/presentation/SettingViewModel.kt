package com.afternote.feature.setting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 설정 화면용 ViewModel.
 *
 * 서버 logout 호출은 best-effort, 로컬 토큰 [AuthRepository.clearSession]은 실패 여부와 무관하게 실행한다.
 */
@HiltViewModel
class SettingViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _logoutCompleted = MutableStateFlow(false)
        val logoutCompleted = _logoutCompleted.asStateFlow()

        fun logout() {
            viewModelScope.launch {
                val refreshToken = authRepository.getRefreshToken().getOrNull()
                if (!refreshToken.isNullOrBlank()) {
                    authRepository.logout(refreshToken)
                }
                authRepository.clearSession()
                _logoutCompleted.value = true
            }
        }
    }
