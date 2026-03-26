package com.afternote.feature.onboarding.data.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    @SerialName("accessToken")
    val accessToken: String,
)
