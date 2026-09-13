package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.ui.mvi.UiState

internal data class DeliveryConditionUiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val conditionType: DeliveryConditionType = DeliveryConditionType.INACTIVITY,
    val inactivityPeriod: InactivityPeriod = InactivityPeriod.ONE_YEAR,
    val conditions: List<DeliveryConditionItem> = emptyList(),
    val error: DeliveryConditionError? = null,
    val isSaving: Boolean = false,
    val pendingEvent: Unit? = null,
) : UiState

enum class DeliveryConditionError {
    LOAD_FAILED,
    SAVE_FAILED,
}
