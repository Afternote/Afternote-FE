package com.afternote.core.domain.usecase

fun interface UserSessionRepository {
    suspend fun getUserId(): Long?
}
