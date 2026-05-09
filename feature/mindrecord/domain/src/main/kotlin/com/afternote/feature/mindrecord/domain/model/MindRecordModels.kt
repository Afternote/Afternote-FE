package com.afternote.feature.mindrecord.domain.model

enum class MindRecordMediaType {
    IMAGE,
    VIDEO,
}

data class MindRecordList(
    val mindRecords: List<MindRecordSummary>,
    val totalCount: Int,
)

data class MindRecordSummary(
    val id: Long,
    val type: MindRecordType,
    val title: String,
    val recordDate: String,
    val isDraft: Boolean,
    val senderName: String,
    val createdAt: String,
)

data class MindRecordDetail(
    val id: Long,
    val type: MindRecordType,
    val title: String,
    val recordDate: String,
    val content: String,
    val senderName: String,
    val createdAt: String,
    val questionId: Long? = null,
    val questionContent: String? = null,
    val category: String? = null,
    val mediaList: List<MindRecordMedia> = emptyList(),
)

data class MindRecordMedia(
    val id: Long,
    val mediaType: MindRecordMediaType,
    val imageUrl: String,
)
