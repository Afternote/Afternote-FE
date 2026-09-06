package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R
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
                            it.copy(isLoading = false, errorMessage = UiText.Resource(R.string.receiver_load_failed))
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
            if (!email.isValidReceiverEmail()) {
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.receiver_email_invalid)) }
                return
            }
            val phoneValidation = phone.validateReceiverPhone(isRequired = true)
            if (phoneValidation != ReceiverPhoneValidation.VALID) {
                val messageRes =
                    if (phoneValidation == ReceiverPhoneValidation.REQUIRED) {
                        R.string.receiver_phone_required
                    } else {
                        R.string.receiver_phone_invalid
                    }
                _uiState.update { it.copy(errorMessage = UiText.Resource(messageRes)) }
                return
            }

            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            viewModelScope.launch {
                val receiverUpdateResult =
                    runCatchingCancellable {
                        userRepository.updateReceiver(
                            receiverId = receiverId,
                            name = name,
                            phone = phone.normalizeReceiverPhone(),
                            relation = relation,
                            email = email.trim(),
                        )
                    }
                receiverUpdateResult.exceptionOrNull()?.let { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.toReceiverFailureMessage(R.string.receiver_edit_failed),
                        )
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
                            errorMessage = UiText.Resource(R.string.receiver_message_update_partial_failed),
                        )
                    }
                }
            }
        }
    }
