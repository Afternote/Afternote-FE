package com.afternote.feature.mindrecord.presentation.model.memoryspace

data class MemoryItem(
    val id: Int,
    val imageUrl: String,
    val title: String,
    val date: String,
    val content: String,
    val tags: List<String>,
)
