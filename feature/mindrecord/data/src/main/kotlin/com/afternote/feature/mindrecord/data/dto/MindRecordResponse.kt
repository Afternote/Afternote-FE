package com.afternote.feature.mindrecord.data.dto

import com.afternote.feature.mindrecord.data.model.MindRecordType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MindRecordResponse(
    @SerialName("records")
    val records: List<Record>,
    @SerialName("markedDates")
    val markedDates: List<String>
)

@Serializable
data class Record(
    @SerialName("recordId")
    val recordId: Int,
    @SerialName("type")
    val type: MindRecordType,
    @SerialName("title")
    val title: String,
    @SerialName("isDraft")
    val isDraft: Boolean
)

@Serializable
data class DailyQuestionResponse(
    val questionId: Long,
    val content: String
)