package com.afternote.core.model.user

data class UserProfileModel(
    val name: String,
    val email: String,
    val phone: String?,
    val profileImageUrl: String?,
)
