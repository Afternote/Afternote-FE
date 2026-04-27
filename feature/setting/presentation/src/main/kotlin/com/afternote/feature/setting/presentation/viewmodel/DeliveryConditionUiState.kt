package com.afternote.feature.setting.presentation.viewmodel

data class DeliveryConditionUiState(
    val selectedDeliveryMethodIndex: Int = 0,
    val selectedProcessingMethodIndex: Int = 0,
    val farewellMessage: String = "",
)
