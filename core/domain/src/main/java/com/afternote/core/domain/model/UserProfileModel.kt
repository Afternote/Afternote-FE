package com.afternote.core.domain.model

data class UserProfile(
    val name: String,
    val email: String,
    val phone: String?,
    val profileImageUrl: String?,
)
