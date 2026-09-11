package com.afternote.feature.setting.presentation.viewmodel

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.PasskeyRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal sealed interface PasskeyRegistrationResult {
    data object Success : PasskeyRegistrationResult

    data object Canceled : PasskeyRegistrationResult

    data class Error(
        val message: UiText,
    ) : PasskeyRegistrationResult
}

@HiltViewModel
internal class PassKeyViewModel
    @Inject
    constructor(
        private val userProfileCacheRepository: UserProfileCacheRepository,
        private val passkeyRepository: PasskeyRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<PassKeyIntent, PassKeyUiState, PassKeyReducerEvent>(PassKeyUiState()) {
        private var registrationJob: Job? = null

        override fun onIntent(intent: PassKeyIntent) {
            when (intent) {
                is PassKeyIntent.Register -> {
                    register(intent.createCredential)
                }

                PassKeyIntent.CancelRegistration -> {
                    registrationJob?.cancel()
                    dispatch(PassKeyReducerEvent.Canceled)
                }

                is PassKeyIntent.ConsumeResult -> {
                    dispatch(PassKeyReducerEvent.Consumed(intent.result))
                }
            }
        }

        override fun reduce(
            state: PassKeyUiState,
            event: PassKeyReducerEvent,
        ): PassKeyUiState =
            when (event) {
                PassKeyReducerEvent.Started -> {
                    state.copy(isRegistering = true, result = null, registrationId = state.registrationId + 1)
                }

                is PassKeyReducerEvent.Finished -> {
                    if (state.registrationId ==
                        event.registrationId
                    ) {
                        state.copy(isRegistering = false, result = event.result)
                    } else {
                        state
                    }
                }

                is PassKeyReducerEvent.Stopped -> {
                    if (state.registrationId == event.registrationId) state.copy(isRegistering = false) else state
                }

                PassKeyReducerEvent.Canceled -> {
                    state.copy(isRegistering = false, result = null, registrationId = state.registrationId + 1)
                }

                is PassKeyReducerEvent.Consumed -> {
                    if (state.result == event.result) state.copy(result = null) else state
                }
            }

        private fun register(createCredential: suspend (String) -> String) {
            if (currentState.isRegistering || currentState.result != null) return
            dispatch(PassKeyReducerEvent.Started)
            val registrationId = currentState.registrationId
            registrationJob =
                viewModelScope.launch {
                    try {
                        dispatch(PassKeyReducerEvent.Finished(registrationId, registerPasskey(createCredential)))
                    } finally {
                        dispatch(PassKeyReducerEvent.Stopped(registrationId))
                    }
                }
        }

        /** Activity를 가진 화면이 플랫폼 요청을 실행한다. 서버 등록이 성공한 뒤에만 완료를 반환한다. */
        private suspend fun registerPasskey(createCredential: suspend (optionsJson: String) -> String): PasskeyRegistrationResult {
            var stage = STAGE_OPTIONS
            return try {
                val options = passkeyRepository.getRegistrationOptions()
                stage = STAGE_CREDENTIAL
                val credential = createCredential(options)
                stage = STAGE_REGISTER
                passkeyRepository.registerPasskey(credential)
                // 캐시 쓰기 실패가 서버 등록 성공을 뒤집거나 재등록을 유도하면 안 된다.
                runCatchingCancellable { userProfileCacheRepository.savePasskeyRegistered(true) }
                    .onFailure { reportFailure(it, STAGE_LOCAL_CACHE) }
                PasskeyRegistrationResult.Success
            } catch (_: CreateCredentialCancellationException) {
                PasskeyRegistrationResult.Canceled
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (failure: Exception) {
                reportFailure(failure, stage)
                PasskeyRegistrationResult.Error(
                    UiText.Resource(
                        if (stage == STAGE_OPTIONS) {
                            R.string.setting_passkey_options_error
                        } else {
                            R.string.setting_passkey_registration_error
                        },
                    ),
                )
            }
        }

        private fun reportFailure(
            failure: Throwable,
            stage: String,
        ) {
            errorReporter.recordFailure(failure, mapOf("stage" to stage))
        }
    }

private const val STAGE_OPTIONS = "passkey_registration_options"
private const val STAGE_CREDENTIAL = "passkey_create_credential"
private const val STAGE_REGISTER = "passkey_register"
private const val STAGE_LOCAL_CACHE = "passkey_registration_cache"
