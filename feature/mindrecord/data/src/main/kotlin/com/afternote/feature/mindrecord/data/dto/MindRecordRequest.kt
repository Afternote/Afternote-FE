package com.afternote.feature.mindrecord.data.dto

import com.afternote.feature.mindrecord.data.model.MindRecordType
import com.afternote.feature.mindrecord.data.model.MindRecordViewType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MindRecordListRequest(
    @SerialName("type")
    val type: MindRecordType,
    @SerialName("view")
    val view: MindRecordViewType,
    @SerialName("year")
    val year: Int?,
    @SerialName("month")
    val month: Int?
) {
    init {
        year?.let {
            require(it >= 0)
        }
        month?.let {
            require(it in 1..12)
        }
    }
}