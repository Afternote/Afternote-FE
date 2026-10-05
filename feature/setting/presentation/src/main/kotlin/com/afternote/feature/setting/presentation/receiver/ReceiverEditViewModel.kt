package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.UpdateReceiverInfoResult
import com.afternote.feature.setting.domain.UpdateReceiverInfoUseCase
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/**
 * 수정 대상 [SettingRoute.RecipientEditRoute] 는 assisted 로 받는다 — Nav3 entry 에는 Nav2 의
 * `savedStateHandle.toRoute<T>()` 자동 채움이 없다 (#1695).
 */
@HiltViewModel(assistedFactory = ReceiverEditViewModel.Factory::class)
internal class ReceiverEditViewModel
    @AssistedInject
    constructor(
        @Assisted route: SettingRoute.RecipientEditRoute,
        private val receiverRepository: UserReceiverRepository,
        private val updateReceiverInfo: UpdateReceiverInfoUseCase,
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
                is ReceiverEditReducerEvent.ValidationFailed -> state.copy(errorMessage = event.message)
                ReceiverEditReducerEvent.Saving -> state.copy(isSaving = true, errorMessage = null)
                ReceiverEditReducerEvent.Saved -> state.copy(isSaving = false, isSaved = true, pendingEvent = ReceiverEditEvent.EditSuccess)
                is ReceiverEditReducerEvent.SaveFailed -> state.copy(isSaving = false, errorMessage = event.message)
                ReceiverEditReducerEvent.SuccessConsumed -> state.copy(pendingEvent = null)
            }

        init {
            loadReceiver()
        }

        private fun loadReceiver() {
            viewModelScope.launch {
                runCatchingCancellable { receiverRepository.getReceiverDetail(receiverId) }
                    .onSuccess { receiver ->
                        dispatch(ReceiverEditReducerEvent.Loaded(receiver))
                    }.onFailure {
                        dispatch(ReceiverEditReducerEvent.LoadFailed(UiText.Resource(R.string.setting_receiver_load_failed)))
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
            if (currentState.isSaveLocked) return
            if (!email.isValidReceiverEmail()) {
                dispatch(ReceiverEditReducerEvent.ValidationFailed(UiText.Resource(R.string.setting_receiver_email_invalid)))
                return
            }
            val phoneValidation = phone.validateReceiverPhone(isRequired = true)
            if (phoneValidation != ReceiverPhoneValidation.VALID) {
                val messageRes =
                    if (phoneValidation == ReceiverPhoneValidation.REQUIRED) {
                        R.string.setting_receiver_phone_required
                    } else {
                        R.string.setting_receiver_phone_invalid
                    }
                dispatch(ReceiverEditReducerEvent.ValidationFailed(UiText.Resource(messageRes)))
                return
            }

            dispatch(ReceiverEditReducerEvent.Saving)
            viewModelScope.launch {
                val result =
                    updateReceiverInfo(
                        receiverId = receiverId,
                        name = name,
                        phone = phone.normalizeReceiverPhone(),
                        relation = relation,
                        email = email.trim(),
                        message = message,
                    )
                when (result) {
                    is UpdateReceiverInfoResult.BasicInfoFailed -> {
                        dispatch(
                            ReceiverEditReducerEvent.SaveFailed(
                                result.cause.toReceiverFailureMessage(R.string.setting_receiver_edit_failed),
                            ),
                        )
                    }

                    UpdateReceiverInfoResult.MessageFailedAfterBasicInfoUpdated -> {
                        dispatch(
                            ReceiverEditReducerEvent.SaveFailed(
                                UiText.Resource(R.string.setting_receiver_message_update_partial_failed),
                            ),
                        )
                    }

                    UpdateReceiverInfoResult.Success -> {
                        dispatch(ReceiverEditReducerEvent.Saved)
                    }
                }
            }
        }

        @AssistedFactory
        interface Factory {
            fun create(route: SettingRoute.RecipientEditRoute): ReceiverEditViewModel
        }
    }
