package com.afternote.feature.setting.presentation.passkey

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
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    val canceledJob = registrationJob
                    registrationJob = null
                    dispatch(PassKeyReducerEvent.Canceled)
                    canceledJob?.cancel()
                }

                is PassKeyIntent.ConsumeResult -> {
                    dispatch(PassKeyReducerEvent.Consumed(intent.registrationId, intent.result))
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
                    if (state.registrationId == event.registrationId && state.isRegistering) {
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
                    if (state.registrationId == event.registrationId && state.result == event.result) state.copy(result = null) else state
                }
            }

        private fun register(createCredential: suspend (String) -> String) {
            if (currentState.isRegistering || currentState.result != null) return
            dispatch(PassKeyReducerEvent.Started)
            val registrationId = currentState.registrationId
            registrationJob =
                viewModelScope.launch {
                    try {
                        registerPasskey(registrationId, createCredential)
                    } finally {
                        dispatch(PassKeyReducerEvent.Stopped(registrationId))
                    }
                }
        }

        /** Activity를 가진 화면이 플랫폼 요청을 실행한다. 서버 등록이 성공한 뒤에만 완료 상태를 확정한다. */
        private suspend fun registerPasskey(
            registrationId: Long,
            createCredential: suspend (optionsJson: String) -> String,
        ) {
            var stage = STAGE_OPTIONS
            try {
                val options = passkeyRepository.getRegistrationOptions()
                currentCoroutineContext().ensureActive()
                stage = STAGE_CREDENTIAL
                val credential = createCredential(options)
                currentCoroutineContext().ensureActive()
                stage = STAGE_REGISTER
                passkeyRepository.registerPasskey(credential)
                // 서버 성공을 먼저 확정한다. 캐시의 실패나 취소는 재등록을 유도하지 않는다.
                dispatch(PassKeyReducerEvent.Finished(registrationId, PasskeyRegistrationResult.Success))
                // 지문 관문이 이 캐시를 본다. 서버에 등록된 뒤에는 화면 이탈·구성 변경으로 job 이 취소돼도
                // 저장을 끝낸다.
                withContext(NonCancellable) {
                    runCatchingCancellable { userProfileCacheRepository.savePasskeyRegistered(true) }
                        .onFailure { reportFailure(it, STAGE_LOCAL_CACHE) }
                }
            } catch (_: CreateCredentialCancellationException) {
                dispatch(PassKeyReducerEvent.Finished(registrationId, PasskeyRegistrationResult.Canceled))
            } catch (canceled: CancellationException) {
                throw canceled
            } catch (failure: Exception) {
                reportFailure(failure, stage)
                dispatch(
                    PassKeyReducerEvent.Finished(
                        registrationId,
                        PasskeyRegistrationResult.Error(
                            UiText.Resource(
                                if (stage == STAGE_OPTIONS) {
                                    R.string.setting_passkey_options_error
                                } else {
                                    R.string.setting_passkey_registration_error
                                },
                            ),
                        ),
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
