package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class DeliveryConditionViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(DeliveryConditionUiState())
        val uiState: StateFlow<DeliveryConditionUiState> = _uiState.asStateFlow()

        fun onSelectDeliveryMethodIndex(index: Int) = _uiState.update { it.copy(selectedDeliveryMethodIndex = index) }

        fun onSelectProcessingMethodIndex(index: Int) = _uiState.update { it.copy(selectedProcessingMethodIndex = index) }

        fun onFarewellMessageChange(message: String) = _uiState.update { it.copy(farewellMessage = message) }
    }
