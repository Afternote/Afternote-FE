package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.domain.PasskeyRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PassKeyListViewModel
    @Inject
    constructor(
        private val passkeyRepository: PasskeyRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PassKeyListUiState(isLoading = true))
        val uiState = _uiState.asStateFlow()
        private var loadJob: Job? = null

        /** 최초 진입·등록 화면에서 복귀·사용자 재시도 모두 서버 목록을 다시 읽는다. */
        fun refresh() {
            if (loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    runCatchingCancellable { passkeyRepository.getPasskeys() }
                        .onSuccess { passkeys ->
                            _uiState.update { it.copy(isLoading = false, passkeys = passkeys) }
                        }.onFailure { failure ->
                            errorReporter.recordFailure(failure, mapOf("stage" to "passkey_list"))
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = UiText.Resource(R.string.setting_passkey_list_error),
                                )
                            }
                        }
                }
        }
    }
