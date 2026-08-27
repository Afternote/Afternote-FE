package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.error.ReceiverRegistrationRejected
import com.afternote.core.domain.repository.UserRepository
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                runCatching {
                    userRepository.createReceiver(
                        name = name,
                        relation = relation,
                        phone = phone?.takeIf { it.isNotBlank() },
                        email = email.trim(),
                        message = message?.takeIf { it.isNotBlank() },
                    )
                }.onSuccess {
                    _events.send(ReceiverRegisterEvent.RegisterSuccess)
                }.onFailure { exception ->
                    val error =
                        (exception as? ReceiverRegistrationRejected)
                            ?.let { ReceiverRegisterError.ServerMessage(it.serverMessage) }
                            ?: ReceiverRegisterError.Generic
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }
            }
        }
    }
