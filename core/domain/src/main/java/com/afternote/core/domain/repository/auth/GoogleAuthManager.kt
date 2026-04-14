package com.afternote.core.domain.repository.auth

fun interface GoogleAuthManager {
    fun getAccessToken(): String?
}
