package com.afternote.core.model.setting

data class ReceiverListItem(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val mindRecordDeliveryEnabled: Boolean = true,
)

data class ReceiverDetail(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String?,
    val email: String?,
    val dailyQuestionCount: Int,
    val timeLetterCount: Int,
    val afterNoteCount: Int,
)
