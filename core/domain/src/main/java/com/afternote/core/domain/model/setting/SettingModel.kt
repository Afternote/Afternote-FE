package com.afternote.core.domain.model.setting

data class PushSettings(
    val timeLetter: Boolean,
    val mindRecord: Boolean,
    val afterNote: Boolean,
)

enum class DeliveryConditionType {
    NONE,
    DEATH_CERTIFICATE,
    INACTIVITY,
    SPECIFIC_DATE,
}

data class DeliveryCondition(
    val conditionType: DeliveryConditionType,
    val inactivityPeriodDays: Int?,
    val specificDate: String?,
    val leaveMessage: String? = null,
    val conditionFulfilled: Boolean,
    val conditionMet: Boolean,
)
