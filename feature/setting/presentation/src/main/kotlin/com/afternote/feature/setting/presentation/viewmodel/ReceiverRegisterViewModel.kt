package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiverRegisterViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReceiverRegisterUiState())
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ReceiverRegisterEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        fun register(
            name: String,
            relation: String,
            phone: String?,
            email: String,
            message: String?,
        ) {
            if (!email.isValidReceiverEmail()) {
                val messageRes =
                    if (email.isBlank()) R.string.receiver_email_required else R.string.receiver_email_invalid
                _uiState.update { it.copy(errorMessage = UiText.Resource(messageRes)) }
                return
            }
            val phoneValidation = phone.orEmpty().validateReceiverPhone(isRequired = true)
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
            val normalizedEmail = email.trim()

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                runCatchingCancellable {
                    userRepository.createReceiver(
                        name = name,
                        relation = relation,
                        phone = phone?.takeIf { it.isNotBlank() }?.normalizeReceiverPhone(),
                        email = normalizedEmail,
                        message = message?.takeIf { it.isNotBlank() },
                    )
                }.onSuccess {
                    _events.send(ReceiverRegisterEvent.RegisterSuccess)
                }.onFailure { error ->
                    val errorMessage = error.toReceiverFailureMessage(R.string.receiver_register_failed)
                    _uiState.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                }
            }
        }
    }
