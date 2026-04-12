package com.afternote.feature.mindrecord.presentation.model.memoryspace

/** 마인드레코드 MemorySpace 화면 전용 UI 모델(도메인 엔티티 아님). */
data class MemoryItem(
    val id: Int,
    val imageRes: Int,
    val title: String,
    val date: String,
    val content: String,
    val tags: List<String>,
)
