package com.afternote.feature.afternote.domain.model.input

data class CreateGalleryInput(
    val title: String,
    val processMethod: String,
    val actions: List<String>,
    val leaveMessage: String? = null,
    val receiverIds: List<Long> = emptyList(),
)
