package com.afternote.core.model

data class ReceiverMindRecordItem(
    val recordId: Long,
    val type: String,
    val titleOrQuestion: String,
    val contentOrAnswer: String,
    val recordDate: String,
)

data class ReceiverMindRecordsResult(
    val items: List<ReceiverMindRecordItem>,
    val hasNext: Boolean,
)
