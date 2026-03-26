package com.afternote.feature.onboarding.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.onboarding.domain.usecase.LoginWithKakaoUseCase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val loginWithKakaoUseCase: LoginWithKakaoUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun loginWithKakao(context: Context) {
            viewModelScope.launch {
                _uiState.value = LoginUiState.Loading
                runCatching {
                    val token = getKakaoOAuthToken(context)
                    loginWithKakaoUseCase(token.accessToken)
                        .getOrThrow()
                }.onSuccess {
                    _uiState.value = LoginUiState.Success
                }.onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.value = LoginUiState.Error(throwable.message ?: "로그인에 실패했습니다.")
                }
            }
        }

        fun clearError() {
            _uiState.value = LoginUiState.Idle
        }

        /**
         * 카카오 SDK 콜백을 코루틴으로 래핑합니다.
         * KakaoTalk 앱이 설치되어 있으면 앱 로그인을 우선 시도하고,
         * 실패하거나 미설치 시 카카오 계정(웹) 로그인으로 폴백합니다.
         */
        private suspend fun getKakaoOAuthToken(context: Context): OAuthToken =
            suspendCancellableCoroutine { cont ->
                val accountLoginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                    when {
                        error != null -> cont.resumeWithException(error)
                        token != null -> cont.resume(token)
                        else -> cont.resumeWithException(IllegalStateException("카카오 로그인 토큰을 받지 못했습니다."))
                    }
                }

                if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                        when {
                            error != null -> {
                                // 사용자 취소는 폴백 없이 바로 실패 처리
                                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                                    cont.resumeWithException(error)
                                } else {
                                    // KakaoTalk 로그인 실패 시 카카오 계정 로그인 폴백
                                    UserApiClient.instance.loginWithKakaoAccount(
                                        context,
                                        callback = accountLoginCallback,
                                    )
                                }
                            }
                            token != null -> cont.resume(token)
                            else -> cont.resumeWithException(IllegalStateException("카카오 로그인 토큰을 받지 못했습니다."))
                        }
                    }
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(
                        context,
                        callback = accountLoginCallback,
                    )
                }
            }
    }

sealed interface LoginUiState {
    data object Idle : LoginUiState

    data object Loading : LoginUiState

    data object Success : LoginUiState

    data class Error(
        val message: String,
    ) : LoginUiState
}
