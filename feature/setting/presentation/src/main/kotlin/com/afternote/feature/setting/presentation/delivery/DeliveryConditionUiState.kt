package com.afternote.feature.setting.presentation.delivery

import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.ui.mvi.UiState

internal data class DeliveryConditionUiState(
    val conditionEditRevision: Int = 0,
    val savedConditionRevision: Int = 0,
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val conditionType: DeliveryConditionType = DeliveryConditionType.INACTIVITY,
    val inactivityPeriod: InactivityPeriod = InactivityPeriod.ONE_YEAR,
    val conditions: List<DeliveryConditionItem> = emptyList(),
    val error: DeliveryConditionError? = null,
    val isSaving: Boolean = false,
    /** 저장을 마쳤다는 사실. 소비되는 [isSaveSuccessPending] 과 달리 되돌리지 않아, 화면이 닫히는 동안에도 재제출을 막는다. */
    val isSaved: Boolean = false,
    /** 화면에 알릴 저장 성공 신호. 어디로 이동할지는 이 신호를 콜백으로 받은 호스트가 정한다. */
    val isSaveSuccessPending: Boolean = false,
) : UiState {
    val isSaveLocked: Boolean get() = isSaving || isSaved
}

enum class DeliveryConditionError {
    LOAD_FAILED,
    SAVE_FAILED,
}
