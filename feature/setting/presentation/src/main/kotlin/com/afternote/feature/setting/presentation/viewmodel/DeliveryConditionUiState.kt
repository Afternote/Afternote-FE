package com.afternote.feature.setting.presentation.viewmodel

import java.time.LocalDate

enum class ProcessingConditionOption { INACTIVITY, SPECIFIC_DATE, RECIPIENT_REQUEST }

data class DeliveryConditionUiState(
    val isLoading: Boolean = false,
    val selectedDeliveryMethodIndex: Int = 0,
    val processingOption: ProcessingConditionOption = ProcessingConditionOption.INACTIVITY,
    val specificDate: LocalDate? = null,
    val isDatePickerVisible: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val farewellMessage: String = "",
)
