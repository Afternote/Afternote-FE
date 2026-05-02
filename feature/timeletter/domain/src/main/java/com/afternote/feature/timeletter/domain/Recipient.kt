package com.afternote.feature.timeletter.domain

data class Recipient(
    val id: Long,
    val name: String,
    val relationship: String,
    val profileImageUrl: String? = null,
)
