package com.afternote.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val loginUseCase: LoginUseCase,
    ) : ViewModel() {
        // UI의 상태를 담는 변수
        private val _uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun login(loginType: LoginType) {
            viewModelScope.launch {
                // 1. 로딩 상태 시작
                _uiState.update { it.copy(isLoading = true, error = null) }

                // 2. 유스케이스 실행
                val loginResult = loginUseCase(loginType)

                // 3. 결과 처리
                loginResult
                    .onSuccess {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    }.onFailure { exception ->
                        _uiState.update { it.copy(isLoading = false, error = exception.message) }
                    }
            }
        }
    }

// UI 상태를 정의하는 데이터 클래스
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)
