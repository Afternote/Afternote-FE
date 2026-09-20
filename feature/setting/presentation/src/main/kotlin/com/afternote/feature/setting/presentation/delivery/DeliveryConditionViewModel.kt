package com.afternote.feature.setting.presentation.delivery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.feature.setting.domain.UpdateTimeLetterDeliveryConditionUseCase
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
internal class DeliveryConditionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val receiverRepository: UserReceiverRepository,
        private val updateTimeLetterDeliveryCondition: UpdateTimeLetterDeliveryConditionUseCase,
    ) : ViewModel() {
        private val receiverId = savedStateHandle.toRoute<SettingRoute.AfterDeliveryRoute>().receiverId

        private val _uiState = MutableStateFlow(DeliveryConditionUiState())
        val uiState: StateFlow<DeliveryConditionUiState> = _uiState.asStateFlow()

        private val _saveSuccess = Channel<Unit>(Channel.BUFFERED)
        val saveSuccess = _saveSuccess.receiveAsFlow()

        init {
            loadDeliveryConditions()
        }

        private fun loadDeliveryConditions() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                runCatchingCancellable { receiverRepository.getReceiverDeliveryConditions(receiverId) }
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

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                runCatchingCancellable {
                    updateTimeLetterDeliveryCondition(
                        receiverId = receiverId,
                        conditions = state.conditions,
                        conditionType = state.conditionType,
                        inactivityPeriod = state.inactivityPeriod,
                    )
                }.onSuccess { response ->
                    _uiState.update { it.copy(isSaving = false, conditions = response.conditions) }
                    _saveSuccess.send(Unit)
                }.onFailure {
                    _uiState.update { it.copy(isSaving = false, error = DeliveryConditionError.SAVE_FAILED) }
                }
            }
        }
    }
