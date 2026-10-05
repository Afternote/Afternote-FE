package com.afternote.feature.setting.presentation.delivery

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.UpdateTimeLetterDeliveryConditionUseCase
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

/**
 * 대상 수신자 [SettingRoute.AfterDeliveryRoute] 는 assisted 로 받는다 — Nav3 entry 에는 Nav2 의
 * `savedStateHandle.toRoute<T>()` 자동 채움이 없다 (#1695).
 */
@HiltViewModel(assistedFactory = DeliveryConditionViewModel.Factory::class)
internal class DeliveryConditionViewModel
    @AssistedInject
    constructor(
        @Assisted route: SettingRoute.AfterDeliveryRoute,
        private val receiverRepository: UserReceiverRepository,
        private val updateTimeLetterDeliveryCondition: UpdateTimeLetterDeliveryConditionUseCase,
    ) : MviViewModel<DeliveryConditionIntent, DeliveryConditionUiState, DeliveryConditionReducerEvent>(DeliveryConditionUiState()) {
        private val receiverId = route.receiverId

        override fun onIntent(intent: DeliveryConditionIntent) {
            when (intent) {
                is DeliveryConditionIntent.SelectConditionType -> dispatch(DeliveryConditionReducerEvent.ConditionSelected(intent.type))
                DeliveryConditionIntent.Save -> onSave()
                DeliveryConditionIntent.ConsumeSuccess -> dispatch(DeliveryConditionReducerEvent.SuccessConsumed)
            }
        }

        override fun reduce(
            state: DeliveryConditionUiState,
            event: DeliveryConditionReducerEvent,
        ): DeliveryConditionUiState =
            when (event) {
                DeliveryConditionReducerEvent.Loading -> {
                    state.copy(isLoading = true)
                }

                is DeliveryConditionReducerEvent.Loaded -> {
                    val representative = event.conditions.firstOrNull { it.contentType == DeliveryContentType.TIME_LETTER }
                    state.copy(
                        isLoading = false,
                        isInitialized = true,
                        conditionType = representative?.conditionType ?: DeliveryConditionType.INACTIVITY,
                        inactivityPeriod = representative?.inactivityPeriod ?: InactivityPeriod.ONE_YEAR,
                        conditions = event.conditions,
                    )
                }

                DeliveryConditionReducerEvent.LoadFailed -> {
                    state.copy(isLoading = false, error = DeliveryConditionError.LOAD_FAILED)
                }

                is DeliveryConditionReducerEvent.ConditionSelected -> {
                    state.copy(conditionType = event.type)
                }

                DeliveryConditionReducerEvent.Saving -> {
                    state.copy(isSaving = true)
                }

                is DeliveryConditionReducerEvent.Saved -> {
                    state.copy(isSaving = false, conditions = event.conditions, isSaved = true, isSaveSuccessPending = true)
                }

                DeliveryConditionReducerEvent.SaveFailed -> {
                    state.copy(isSaving = false, error = DeliveryConditionError.SAVE_FAILED)
                }

                DeliveryConditionReducerEvent.SuccessConsumed -> {
                    state.copy(isSaveSuccessPending = false)
                }
            }

        init {
            loadDeliveryConditions()
        }

        private fun loadDeliveryConditions() {
            viewModelScope.launch {
                dispatch(DeliveryConditionReducerEvent.Loading)
                runCatchingCancellable { receiverRepository.getReceiverDeliveryConditions(receiverId) }
                    .onSuccess { response ->
                        dispatch(DeliveryConditionReducerEvent.Loaded(response.conditions))
                    }.onFailure {
                        dispatch(DeliveryConditionReducerEvent.LoadFailed)
                    }
            }
        }

        private fun onSave() {
            val state = currentState
            if (!state.isInitialized || state.isSaveLocked) return

            dispatch(DeliveryConditionReducerEvent.Saving)
            viewModelScope.launch {
                runCatchingCancellable {
                    updateTimeLetterDeliveryCondition(
                        receiverId = receiverId,
                        conditions = state.conditions,
                        conditionType = state.conditionType,
                        inactivityPeriod = state.inactivityPeriod,
                    )
                }.onSuccess { response ->
                    dispatch(DeliveryConditionReducerEvent.Saved(response.conditions))
                }.onFailure {
                    dispatch(DeliveryConditionReducerEvent.SaveFailed)
                }
            }
        }

        @AssistedFactory
        interface Factory {
            fun create(route: SettingRoute.AfterDeliveryRoute): DeliveryConditionViewModel
        }
    }
