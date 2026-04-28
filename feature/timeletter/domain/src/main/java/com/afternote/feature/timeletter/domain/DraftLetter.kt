package com.afternote.feature.timeletter.domain

data class DraftLetter(
    val id: Long,
    val title: String?,
    val recipientName: String?,
    val opendate: String?,
)
