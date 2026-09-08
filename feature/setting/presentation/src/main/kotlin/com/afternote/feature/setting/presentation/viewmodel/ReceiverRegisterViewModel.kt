package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReceiverRegisterViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : MviViewModel<ReceiverRegisterIntent, ReceiverRegisterUiState, ReceiverRegisterReducerEvent>(ReceiverRegisterUiState()) {
        override fun onIntent(intent: ReceiverRegisterIntent) {
            when (intent) {
                is ReceiverRegisterIntent.Register -> register(intent.name, intent.relation, intent.phone, intent.email, intent.message)
                ReceiverRegisterIntent.ConsumeSuccess -> dispatch(ReceiverRegisterReducerEvent.SuccessConsumed)
            }
        }

        override fun reduce(
            state: ReceiverRegisterUiState,
            event: ReceiverRegisterReducerEvent,
        ): ReceiverRegisterUiState =
            when (event) {
                ReceiverRegisterReducerEvent.Registering -> {
                    state.copy(isLoading = true, errorMessage = null)
                }

                ReceiverRegisterReducerEvent.Registered -> {
                    state.copy(
                        isLoading = false,
                        pendingEvent = ReceiverRegisterEvent.RegisterSuccess,
                    )
                }

                is ReceiverRegisterReducerEvent.Failed -> {
                    state.copy(isLoading = false, errorMessage = event.message)
                }

                ReceiverRegisterReducerEvent.SuccessConsumed -> {
                    state.copy(pendingEvent = null)
                }
            }

        private fun register(
            name: String,
            relation: String,
            phone: String?,
            email: String,
            message: String?,
        ) {
            if (currentState.isLoading || currentState.pendingEvent != null) return
            if (!email.isValidReceiverEmail()) {
                val messageRes =
                    if (email.isBlank()) R.string.receiver_email_required else R.string.receiver_email_invalid
                dispatch(ReceiverRegisterReducerEvent.Failed(UiText.Resource(messageRes)))
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
                dispatch(ReceiverRegisterReducerEvent.Failed(UiText.Resource(messageRes)))
                return
            }
            val normalizedEmail = email.trim()

            dispatch(ReceiverRegisterReducerEvent.Registering)
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
                    dispatch(ReceiverRegisterReducerEvent.Registered)
                }.onFailure { error ->
                    val errorMessage = error.toReceiverFailureMessage(R.string.receiver_register_failed)
                    dispatch(ReceiverRegisterReducerEvent.Failed(errorMessage))
                }
            }
        }
    }
