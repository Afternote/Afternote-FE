package com.afternote.feature.setting.presentation.delivery

import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface DeliveryConditionIntent : MviIntent {
    data class SelectConditionType(
        val index: Int,
    ) : DeliveryConditionIntent

    data object Save : DeliveryConditionIntent

    data object ConsumeSuccess : DeliveryConditionIntent
}

internal sealed interface DeliveryConditionReducerEvent : ReducerEvent {
    data object Loading : DeliveryConditionReducerEvent

    data class Loaded(
        val conditions: List<DeliveryConditionItem>,
    ) : DeliveryConditionReducerEvent

    data object LoadFailed : DeliveryConditionReducerEvent

    data class ConditionSelected(
        val type: DeliveryConditionType,
    ) : DeliveryConditionReducerEvent

    data object Saving : DeliveryConditionReducerEvent

    data class Saved(
        val conditions: List<DeliveryConditionItem>,
    ) : DeliveryConditionReducerEvent

    data object SaveFailed : DeliveryConditionReducerEvent

    data object SuccessConsumed : DeliveryConditionReducerEvent
}
