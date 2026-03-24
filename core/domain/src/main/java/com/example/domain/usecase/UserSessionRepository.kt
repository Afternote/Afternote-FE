package com.example.domain.usecase

fun interface UserSessionRepository {
    fun getUserId(): Long?
}
