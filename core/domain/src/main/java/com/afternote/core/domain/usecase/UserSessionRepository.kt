package com.afternote.core.domain.usecase

fun interface UserSessionRepository {
    fun getUserId(): Long?
}
