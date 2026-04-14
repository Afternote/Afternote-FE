package com.afternote.feature.onboarding.presentation.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState = _uiState.asStateFlow()

        val emailState = TextFieldState()
        val passwordState = TextFieldState()

        fun loginWithEmail() {
            login(
                LoginType.Email(
                    email = emailState.text.toString(),
                    password = passwordState.text.toString(),
                ),
            )
        }

        fun login(loginType: LoginType) {
            if (_uiState.value.isLoading) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val loginResult =
                    loginUseCase(
                        loginType = loginType,
                    )

                loginResult
                    .onSuccess {
                        _uiState.update {
                            it.copy(isLoading = false, isSuccess = true)
                        }
                    }.onFailure { exception ->
                        _uiState.update {
                            it.copy(isLoading = false, error = exception.message)
                        }
                    }
            }
        }
    }
