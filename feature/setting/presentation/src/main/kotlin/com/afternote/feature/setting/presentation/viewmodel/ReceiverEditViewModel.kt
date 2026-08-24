package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiverEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val receiverId = savedStateHandle.toRoute<SettingRoute.RecipientEditRoute>().receiverId

        private val _uiState = MutableStateFlow(ReceiverEditUiState())
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ReceiverEditEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            loadReceiver()
        }

        private fun loadReceiver() {
            viewModelScope.launch {
                runCatchingCancellable { userRepository.getReceiverDetail(receiverId) }
                    .onSuccess { receiver ->
                        _uiState.update { it.copy(isLoading = false, receiver = receiver) }
                    }.onFailure {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "수신자 정보를 불러오지 못했습니다.")
                        }
                    }
            }
        }

        fun update(
            name: String,
            relation: String,
            phone: String,
            email: String,
            message: String,
        ) {
            if (_uiState.value.isSaving) return

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            viewModelScope.launch {
                val receiverUpdateResult =
                    runCatchingCancellable {
                        userRepository.updateReceiver(
                            receiverId = receiverId,
                            name = name,
                            phone = phone,
                            relation = relation,
                            email = email,
                        )
                    }
                if (receiverUpdateResult.isFailure) {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "수신자 수정에 실패했습니다.")
                    }
                    return@launch
                }

                runCatchingCancellable {
                    userRepository.updateReceiverMessage(
                        receiverId = receiverId,
                        message = message,
                    )
                }.onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(ReceiverEditEvent.EditSuccess)
                }.onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "기본 정보는 수정됐지만 마지막 인사말 수정에 실패했습니다.",
                        )
                    }
                }
            }
        }
    }
