package com.afternote.feature.onboarding.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

        private val eventChannel = Channel<LoginEvent>(Channel.BUFFERED)
        val eventFlow: Flow<LoginEvent> = eventChannel.receiveAsFlow()

        var email: String by mutableStateOf("")
            private set
        var password: String by mutableStateOf("")
            private set

        fun updateEmail(value: String) {
            email = value
        }

        fun updatePassword(value: String) {
            password = value
        }

        fun loginWithEmail() {
            login(
                LoginType.Email(
                    email = email,
                    password = password,
                ),
            )
        }

        fun loginWithKakao(oauthToken: String) {
            login(LoginType.Kakao(oauthToken))
        }

        fun loginWithGoogle(idToken: String) {
            login(LoginType.Google(idToken))
        }

        private fun login(loginType: LoginType) {
            if (_uiState.value.isLoading) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val result = loginUseCase(loginType = loginType)
                _uiState.update { it.copy(isLoading = false) }

                result
                    .onSuccess {
                        eventChannel.send(LoginEvent.LoginSuccess)
                    }.onFailure { exception ->
                        eventChannel.send(
                            LoginEvent.ShowError(exception.message),
                        )
                    }
            }
        }
    }
