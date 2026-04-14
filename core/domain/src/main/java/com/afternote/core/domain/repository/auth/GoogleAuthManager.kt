package com.afternote.core.domain.repository.auth

import android.app.Activity

interface GoogleAuthManager {
    suspend fun login(activity: Activity): Result<String>
}
