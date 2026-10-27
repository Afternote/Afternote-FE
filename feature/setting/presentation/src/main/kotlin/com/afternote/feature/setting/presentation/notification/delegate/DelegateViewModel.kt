package com.afternote.feature.setting.presentation.notification.delegate

import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.NotificationDelegate
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.receiver.ReceiverPhoneValidation
import com.afternote.feature.setting.presentation.receiver.normalizeReceiverPhone
import com.afternote.feature.setting.presentation.receiver.validateReceiverPhone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 대리인을 지정·변경·해제한다 (#2321).
 *
 * 대리인은 사용자가 오래 접속하지 않을 때 서버가 연락하는 사람이다. 수신인과 별개이고, 연락만 받을 뿐
 * 기록을 열람하지 않는다. 연락처 형식은 수신인과 같은 규칙으로 검사한다.
 */
@HiltViewModel
internal class DelegateViewModel
    @Inject
    constructor(
        private val repository: NotificationPreferenceRepository,
    ) : MviViewModel<DelegateIntent, DelegateUiState, DelegateReducerEvent>(DelegateUiState()) {
        init {
            load()
        }

        override fun onIntent(intent: DelegateIntent) {
            when (intent) {
                DelegateIntent.Retry -> load()
                is DelegateIntent.Save -> save(intent.name, intent.phone)
                DelegateIntent.Remove -> remove()
                DelegateIntent.ConsumeDone -> dispatch(DelegateReducerEvent.DoneConsumed)
                DelegateIntent.ConsumeError -> dispatch(DelegateReducerEvent.ErrorConsumed)
            }
        }

        override fun reduce(
            state: DelegateUiState,
            event: DelegateReducerEvent,
        ): DelegateUiState =
            when (event) {
                DelegateReducerEvent.Loading -> state.copy(isLoading = true, loadFailed = false)
                is DelegateReducerEvent.Loaded -> state.copy(isLoading = false, delegate = event.delegate)
                DelegateReducerEvent.LoadFailed -> state.copy(isLoading = false, loadFailed = true)
                DelegateReducerEvent.Submitting -> state.copy(isSubmitting = true, errorMessage = null)
                is DelegateReducerEvent.Done -> state.copy(isSubmitting = false, delegate = event.delegate, doneMessage = event.message)
                is DelegateReducerEvent.Failed -> state.copy(isSubmitting = false, errorMessage = event.message)
                DelegateReducerEvent.DoneConsumed -> state.copy(doneMessage = null)
                DelegateReducerEvent.ErrorConsumed -> state.copy(errorMessage = null)
            }

        private fun load() {
            dispatch(DelegateReducerEvent.Loading)
            viewModelScope.launch {
                repository
                    .getDelegate()
                    .onSuccess { dispatch(DelegateReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(DelegateReducerEvent.LoadFailed) }
            }
        }

        private fun save(
            name: String,
            phone: String,
        ) {
            if (currentState.isSubmitting) return
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                dispatch(DelegateReducerEvent.Failed(UiText.Resource(R.string.setting_delegate_name_required)))
                return
            }
            when (phone.validateReceiverPhone(isRequired = true)) {
                ReceiverPhoneValidation.VALID -> {
                    Unit
                }

                ReceiverPhoneValidation.REQUIRED -> {
                    dispatch(DelegateReducerEvent.Failed(UiText.Resource(R.string.setting_delegate_phone_required)))
                    return
                }

                ReceiverPhoneValidation.INVALID -> {
                    dispatch(DelegateReducerEvent.Failed(UiText.Resource(R.string.setting_delegate_phone_invalid)))
                    return
                }
            }
            val delegate = NotificationDelegate(name = trimmedName, phone = phone.normalizeReceiverPhone())
            dispatch(DelegateReducerEvent.Submitting)
            viewModelScope.launch {
                repository
                    .updateDelegate(delegate)
                    .onSuccess { dispatch(DelegateReducerEvent.Done(delegate, UiText.Resource(R.string.setting_delegate_saved))) }
                    .onFailure { dispatch(DelegateReducerEvent.Failed(UiText.Resource(R.string.setting_delegate_save_failed))) }
            }
        }

        private fun remove() {
            if (currentState.isSubmitting || currentState.delegate == null) return
            dispatch(DelegateReducerEvent.Submitting)
            viewModelScope.launch {
                repository
                    .deleteDelegate()
                    .onSuccess { dispatch(DelegateReducerEvent.Done(null, UiText.Resource(R.string.setting_delegate_removed))) }
                    .onFailure { dispatch(DelegateReducerEvent.Failed(UiText.Resource(R.string.setting_delegate_save_failed))) }
            }
        }
    }
