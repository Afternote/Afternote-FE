package com.afternote.core.domain.repository

fun interface KakaoAuthManager {
    fun getAccessToken(): String?
}
