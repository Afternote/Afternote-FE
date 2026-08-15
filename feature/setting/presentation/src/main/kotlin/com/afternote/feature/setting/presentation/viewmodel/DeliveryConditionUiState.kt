package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod

data class DeliveryConditionUiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val conditionType: DeliveryConditionType = DeliveryConditionType.INACTIVITY,
    val inactivityPeriod: InactivityPeriod = InactivityPeriod.ONE_YEAR,
    val conditions: List<DeliveryConditionItem> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)
