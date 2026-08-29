package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PassKeyListViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PassKeyListUiState(isLoading = true))
        val uiState = _uiState.asStateFlow()

        init {
            loadPasskeys()
        }

        fun retry() {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            loadPasskeys()
        }

        private fun loadPasskeys() {
            viewModelScope.launch {
                runCatching { userRepository.getPasskeys() }
                    .onSuccess { passkeys ->
                        _uiState.update { it.copy(isLoading = false, passkeys = passkeys) }
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "패스키 목록을 불러올 수 없습니다.") }
                    }
            }
        }
    }
