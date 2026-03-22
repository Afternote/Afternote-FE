package com.afternote.feature.mindrecord.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MindRecordType {
    @SerialName("DAILY_QUESTION")
    DAILY_QUESTION,
    @SerialName("DIARY")
    DIARY,
    @SerialName("DEEP_THOUGHT")
    DEEP_THOUGHT
}