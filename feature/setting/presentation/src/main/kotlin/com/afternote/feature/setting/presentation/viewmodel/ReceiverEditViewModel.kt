package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ReceiverEditViewModel.Factory::class)
internal class ReceiverEditViewModel
    @AssistedInject
    constructor(
        @Assisted route: SettingRoute.RecipientEditRoute,
        private val userRepository: UserRepository,
    ) : MviViewModel<ReceiverEditIntent, ReceiverEditUiState, ReceiverEditReducerEvent>(ReceiverEditUiState()) {
        private val receiverId = route.receiverId

        override fun onIntent(intent: ReceiverEditIntent) {
            when (intent) {
                is ReceiverEditIntent.Update -> update(intent.name, intent.relation, intent.phone, intent.email, intent.message)
                ReceiverEditIntent.ConsumeSuccess -> dispatch(ReceiverEditReducerEvent.SuccessConsumed)
            }
        }

        override fun reduce(
            state: ReceiverEditUiState,
            event: ReceiverEditReducerEvent,
        ): ReceiverEditUiState =
            when (event) {
                is ReceiverEditReducerEvent.Loaded -> state.copy(isLoading = false, receiver = event.receiver)
                is ReceiverEditReducerEvent.LoadFailed -> state.copy(isLoading = false, errorMessage = event.message)
                ReceiverEditReducerEvent.Saving -> state.copy(isSaving = true, errorMessage = null)
                ReceiverEditReducerEvent.Saved -> state.copy(isSaving = false, pendingEvent = ReceiverEditEvent.EditSuccess)
                is ReceiverEditReducerEvent.SaveFailed -> state.copy(isSaving = false, errorMessage = event.message)
                ReceiverEditReducerEvent.SuccessConsumed -> state.copy(pendingEvent = null)
            }

        init {
            loadReceiver()
        }

        private fun loadReceiver() {
            viewModelScope.launch {
                runCatchingCancellable { userRepository.getReceiverDetail(receiverId) }
                    .onSuccess { receiver ->
                        dispatch(ReceiverEditReducerEvent.Loaded(receiver))
                    }.onFailure {
                        dispatch(ReceiverEditReducerEvent.LoadFailed(UiText.Resource(R.string.receiver_load_failed)))
                    }
            }
        }

        private fun update(
            name: String,
            relation: String,
            phone: String,
            email: String,
            message: String,
        ) {
            if (currentState.isSaving || currentState.pendingEvent != null) return
            if (!email.isValidReceiverEmail()) {
                dispatch(ReceiverEditReducerEvent.SaveFailed(UiText.Resource(R.string.receiver_email_invalid)))
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
                dispatch(ReceiverEditReducerEvent.SaveFailed(UiText.Resource(messageRes)))
                return
            }

            dispatch(ReceiverEditReducerEvent.Saving)
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
                    dispatch(ReceiverEditReducerEvent.SaveFailed(error.toReceiverFailureMessage(R.string.receiver_edit_failed)))
                    return@launch
                }

                runCatchingCancellable {
                    userRepository.updateReceiverMessage(
                        receiverId = receiverId,
                        message = message,
                    )
                }.onSuccess {
                    dispatch(ReceiverEditReducerEvent.Saved)
                }.onFailure {
                    dispatch(ReceiverEditReducerEvent.SaveFailed(UiText.Resource(R.string.receiver_message_update_partial_failed)))
                }
            }
        }

        @AssistedFactory
        interface Factory {
            fun create(route: SettingRoute.RecipientEditRoute): ReceiverEditViewModel
        }
    }
