package com.afternote.feature.setting.presentation.passkey

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
import kotlinx.coroutines.launch

@HiltViewModel
internal class PassKeyListViewModel
    @Inject
    constructor(
        private val passkeyRepository: PasskeyRepository,
        private val userProfileCacheRepository: UserProfileCacheRepository,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<PassKeyListIntent, PassKeyListUiState, PassKeyListReducerEvent>(PassKeyListUiState(isLoading = true)) {
        override fun onIntent(intent: PassKeyListIntent) {
            when (intent) {
                PassKeyListIntent.Refresh -> refresh()
            }
        }

        override fun reduce(
            state: PassKeyListUiState,
            event: PassKeyListReducerEvent,
        ): PassKeyListUiState =
            when (event) {
                PassKeyListReducerEvent.Loading -> {
                    state.copy(isLoading = true, errorMessage = null)
                }

                is PassKeyListReducerEvent.Loaded -> {
                    state.copy(isLoading = false, passkeys = event.passkeys)
                }

                PassKeyListReducerEvent.Failed -> {
                    state.copy(
                        isLoading = false,
                        errorMessage = UiText.Resource(R.string.setting_passkey_list_error),
                    )
                }
            }

        private var loadJob: Job? = null
        private var requestId = 0L

        /** 최초 진입·등록 화면에서 복귀·사용자 재시도 모두 서버 목록을 다시 읽는다. */
        private fun refresh() {
            val currentRequestId = ++requestId
            loadJob?.cancel()
            dispatch(PassKeyListReducerEvent.Loading)
            loadJob =
                viewModelScope.launch {
                    runCatchingCancellable { passkeyRepository.getPasskeys() }
                        .onSuccess { passkeys ->
                            if (requestId == currentRequestId) dispatch(PassKeyListReducerEvent.Loaded(passkeys))
                            if (passkeys.isNotEmpty()) syncRegisteredCache()
                        }.onFailure { failure ->
                            if (requestId == currentRequestId) {
                                errorReporter.recordFailure(failure, mapOf("stage" to "passkey_list"))
                                dispatch(PassKeyListReducerEvent.Failed)
                            }
                        }
                }
        }

        /**
         * 등록 직후 캐시 저장이 빠진 기기를 서버 목록으로 되살린다. 지문 관문을 켜는 방향으로만 맞춘다 —
         * 빈 목록으로 캐시를 내리면 목록 응답 하나가 관문을 끌 수 있다.
         */
        private suspend fun syncRegisteredCache() {
            runCatchingCancellable { userProfileCacheRepository.savePasskeyRegistered(true) }
                .onFailure { errorReporter.recordFailure(it, mapOf("stage" to "passkey_list_cache")) }
        }
    }
