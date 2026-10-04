package com.afternote.feature.setting.presentation.delivery

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.ui.mvi.MviViewModel
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
                    state.copy(isSaving = false, conditions = event.conditions, isSaved = true, shouldNavigateBack = true)
                }

                DeliveryConditionReducerEvent.SaveFailed -> {
                    state.copy(isSaving = false, error = DeliveryConditionError.SAVE_FAILED)
                }

                DeliveryConditionReducerEvent.SuccessConsumed -> {
                    state.copy(shouldNavigateBack = false)
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

            val hasTimeLetterCondition =
                state.conditions.any { it.contentType == DeliveryContentType.TIME_LETTER }
            val updatedConditions =
                state.conditions
                    .map { condition ->
                        if (condition.contentType == DeliveryContentType.TIME_LETTER) {
                            condition.copy(
                                conditionType = state.conditionType,
                                inactivityPeriod =
                                    state.inactivityPeriod.takeIf {
                                        state.conditionType == DeliveryConditionType.INACTIVITY
                                    },
                            )
                        } else {
                            condition
                        }
                    }.let { conditions ->
                        if (hasTimeLetterCondition) {
                            conditions
                        } else {
                            conditions +
                                defaultCondition(DeliveryContentType.TIME_LETTER).copy(
                                    conditionType = state.conditionType,
                                    inactivityPeriod =
                                        state.inactivityPeriod.takeIf {
                                            state.conditionType == DeliveryConditionType.INACTIVITY
                                        },
                                )
                        }
                    }

            dispatch(DeliveryConditionReducerEvent.Saving)
            viewModelScope.launch {
                runCatchingCancellable {
                    receiverRepository.updateReceiverDeliveryConditions(receiverId, updatedConditions)
                }.onSuccess { response ->
                    dispatch(DeliveryConditionReducerEvent.Saved(response.conditions))
                }.onFailure {
                    dispatch(DeliveryConditionReducerEvent.SaveFailed)
                }
            }
        }

        private fun defaultCondition(contentType: DeliveryContentType) =
            DeliveryConditionItem(
                contentType = contentType,
                conditionType = DeliveryConditionType.INACTIVITY,
                inactivityPeriod = InactivityPeriod.ONE_YEAR,
                state = ConditionState.ACTIVE,
                fulfilled = false,
                gracePeriodStartedAt = null,
                fulfilledAt = null,
            )

        @AssistedFactory
        interface Factory {
            fun create(route: SettingRoute.AfterDeliveryRoute): DeliveryConditionViewModel
        }
    }
