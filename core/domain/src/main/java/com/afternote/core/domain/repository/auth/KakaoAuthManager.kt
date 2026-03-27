package com.afternote.core.domain.repository.auth

fun interface KakaoAuthManager {
    fun getAccessToken(): String?
}
