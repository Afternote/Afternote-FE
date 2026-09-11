package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.PasskeyRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PassKeyListViewModel
    @Inject
    constructor(
        private val passkeyRepository: PasskeyRepository,
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

        /** 최초 진입·등록 화면에서 복귀·사용자 재시도 모두 서버 목록을 다시 읽는다. */
        private fun refresh() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    dispatch(PassKeyListReducerEvent.Loading)
                    runCatchingCancellable { passkeyRepository.getPasskeys() }
                        .onSuccess { passkeys ->
                            dispatch(PassKeyListReducerEvent.Loaded(passkeys))
                        }.onFailure { failure ->
                            errorReporter.recordFailure(failure, mapOf("stage" to "passkey_list"))
                            dispatch(PassKeyListReducerEvent.Failed)
                        }
                }
        }
    }
