package com.afternote.core.domain.repository

fun interface AuthRepository {
    suspend fun getUserId(): Long?
}
