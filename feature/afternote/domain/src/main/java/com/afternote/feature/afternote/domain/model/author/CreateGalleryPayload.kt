package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class CreateGalleryPayload(
    val title: String,
    val processingMethods: List<String>,
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val receiverIds: List<Long> = emptyList(),
    /** true 면 임시저장으로 만든다 — 서버가 카테고리별 필수값 검증을 건너뛴다(BE `AfternoteValidator`). */
    val isDraft: Boolean = false,
)
