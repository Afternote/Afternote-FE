package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.result.runCatchingCancellable
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
        private val receiverRepository: UserReceiverRepository,
    ) : MviViewModel<ReceiverRegisterIntent, ReceiverRegisterUiState, ReceiverRegisterReducerEvent>(ReceiverRegisterUiState()) {
        override fun onIntent(intent: ReceiverRegisterIntent) {
            when (intent) {
                is ReceiverRegisterIntent.Register -> register(intent)
                is ReceiverRegisterIntent.ConsumeEvent -> dispatch(ReceiverRegisterReducerEvent.EventConsumed(intent.event))
            }
        }

        override fun reduce(
            state: ReceiverRegisterUiState,
            event: ReceiverRegisterReducerEvent,
        ): ReceiverRegisterUiState =
            when (event) {
                ReceiverRegisterReducerEvent.RegisterStarted -> {
                    state.copy(isLoading = true, errorMessage = null)
                }

                ReceiverRegisterReducerEvent.RegisterSucceeded -> {
                    state.copy(isLoading = false, pendingEvent = ReceiverRegisterEvent.RegisterSuccess)
                }

                is ReceiverRegisterReducerEvent.RegisterFailed -> {
                    state.copy(isLoading = false, errorMessage = event.message)
                }

                is ReceiverRegisterReducerEvent.EventConsumed -> {
                    if (state.pendingEvent == event.event) state.copy(pendingEvent = null) else state
                }
            }

        private fun register(intent: ReceiverRegisterIntent.Register) {
            if (currentState.isLoading) return
            val name = intent.name
            val relation = intent.relation
            val phone = intent.phone
            val email = intent.email
            val message = intent.message
            if (!email.isValidReceiverEmail()) {
                val messageRes =
                    if (email.isBlank()) R.string.setting_receiver_email_required else R.string.setting_receiver_email_invalid
                dispatch(ReceiverRegisterReducerEvent.RegisterFailed(UiText.Resource(messageRes)))
                return
            }
            val phoneValidation = phone.orEmpty().validateReceiverPhone(isRequired = true)
            if (phoneValidation != ReceiverPhoneValidation.VALID) {
                val messageRes =
                    if (phoneValidation == ReceiverPhoneValidation.REQUIRED) {
                        R.string.setting_receiver_phone_required
                    } else {
                        R.string.setting_receiver_phone_invalid
                    }
                dispatch(ReceiverRegisterReducerEvent.RegisterFailed(UiText.Resource(messageRes)))
                return
            }
            val normalizedEmail = email.trim()

            dispatch(ReceiverRegisterReducerEvent.RegisterStarted)
            viewModelScope.launch {
                runCatchingCancellable {
                    receiverRepository.createReceiver(
                        name = name,
                        relation = relation,
                        phone = phone?.takeIf { it.isNotBlank() }?.normalizeReceiverPhone(),
                        email = normalizedEmail,
                        message = message?.takeIf { it.isNotBlank() },
                    )
                }.onSuccess {
                    dispatch(ReceiverRegisterReducerEvent.RegisterSucceeded)
                }.onFailure { error ->
                    val errorMessage = error.toReceiverFailureMessage(R.string.setting_receiver_register_failed)
                    dispatch(ReceiverRegisterReducerEvent.RegisterFailed(errorMessage))
                }
            }
        }
    }
