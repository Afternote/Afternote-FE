package com.afternote.feature.mindrecord.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MindRecordViewType {
    @SerialName("LIST")
    LIST,

    @SerialName("CALENDAR")
    CALENDAR,
}
