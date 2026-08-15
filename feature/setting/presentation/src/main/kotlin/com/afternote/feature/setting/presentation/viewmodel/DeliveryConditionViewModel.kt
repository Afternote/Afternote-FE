package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryConditionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val receiverId = savedStateHandle.toRoute<SettingRoute.AfterDeliveryRoute>().receiverId

        private val _uiState = MutableStateFlow(DeliveryConditionUiState())
        val uiState: StateFlow<DeliveryConditionUiState> = _uiState.asStateFlow()

        private val _saveSuccess = Channel<Unit>(Channel.BUFFERED)
        val saveSuccess = _saveSuccess.receiveAsFlow()

        init {
            loadDeliveryConditions()
        }

        fun loadDeliveryConditions() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                runCatchingCancellable { userRepository.getReceiverDeliveryConditions(receiverId) }
                    .onSuccess { response ->
                        val representative =
                            response.conditions.firstOrNull {
                                it.contentType == DeliveryContentType.TIME_LETTER
                            }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isInitialized = true,
                                conditionType = representative?.conditionType ?: DeliveryConditionType.INACTIVITY,
                                inactivityPeriod = representative?.inactivityPeriod ?: InactivityPeriod.ONE_YEAR,
                                conditions = response.conditions,
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false, error = DeliveryConditionError.LOAD_FAILED) }
                    }
            }
        }

        fun onConditionTypeSelected(index: Int) {
            val conditionType =
                if (index == 1) DeliveryConditionType.RECEIVER_REQUEST else DeliveryConditionType.INACTIVITY
            _uiState.update { it.copy(conditionType = conditionType) }
        }

        fun onSave() {
            val state = _uiState.value
            if (!state.isInitialized || state.isSaving) return

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

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                runCatchingCancellable {
                    userRepository.updateReceiverDeliveryConditions(receiverId, updatedConditions)
                }.onSuccess { response ->
                    _uiState.update { it.copy(isSaving = false, conditions = response.conditions) }
                    _saveSuccess.send(Unit)
                }.onFailure {
                    _uiState.update { it.copy(isSaving = false, error = DeliveryConditionError.SAVE_FAILED) }
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
    }
