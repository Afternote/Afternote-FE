package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.DeliveryConditionType
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@HiltViewModel
class DeliveryConditionViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeliveryConditionUiState())
        val uiState: StateFlow<DeliveryConditionUiState> = _uiState.asStateFlow()

        private val _saveSuccess = Channel<Unit>(Channel.BUFFERED)
        val saveSuccess = _saveSuccess.receiveAsFlow()

        init {
            loadDeliveryCondition()
        }

        private fun loadDeliveryCondition() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                runCatching { userRepository.getDeliveryCondition() }
                    .onSuccess { condition ->
                        val option =
                            when (condition.conditionType) {
                                DeliveryConditionType.INACTIVITY -> ProcessingConditionOption.INACTIVITY
                                DeliveryConditionType.SPECIFIC_DATE -> ProcessingConditionOption.SPECIFIC_DATE
                                DeliveryConditionType.NONE -> ProcessingConditionOption.INACTIVITY
                            }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                processingOption = option,
                                specificDate = condition.specificDate?.let { date -> LocalDate.parse(date) },
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false) }
                    }
            }
        }

        fun onSelectDeliveryMethodIndex(index: Int) = _uiState.update { it.copy(selectedDeliveryMethodIndex = index) }

        fun onConditionTypeSelected(option: ProcessingConditionOption) {
            _uiState.update {
                it.copy(
                    processingOption = option,
                    specificDate = if (option != ProcessingConditionOption.SPECIFIC_DATE) null else it.specificDate,
                    isDatePickerVisible = option == ProcessingConditionOption.SPECIFIC_DATE,
                )
            }
        }

        fun onDateSelected(date: LocalDate) {
            _uiState.update { it.copy(specificDate = date, isDatePickerVisible = false) }
        }

        fun onDatePickerToggle(visible: Boolean) {
            _uiState.update { it.copy(isDatePickerVisible = visible) }
        }

        fun onFarewellMessageChange(message: String) = _uiState.update { it.copy(farewellMessage = message) }

        fun onSave() {
            val state = _uiState.value
            val conditionType =
                when (state.processingOption) {
                    ProcessingConditionOption.INACTIVITY -> DeliveryConditionType.INACTIVITY
                    ProcessingConditionOption.SPECIFIC_DATE -> DeliveryConditionType.SPECIFIC_DATE
                    ProcessingConditionOption.RECIPIENT_REQUEST -> return
                }
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                runCatching {
                    userRepository.updateDeliveryCondition(
                        conditionType = conditionType,
                        inactivityPeriodDays = if (state.processingOption == ProcessingConditionOption.INACTIVITY) 365 else null,
                        specificDate =
                            if (state.processingOption == ProcessingConditionOption.SPECIFIC_DATE) {
                                state.specificDate?.toString()
                            } else {
                                null
                            },
                    )
                }.onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _saveSuccess.send(Unit)
                }.onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
                }
            }
        }
    }
