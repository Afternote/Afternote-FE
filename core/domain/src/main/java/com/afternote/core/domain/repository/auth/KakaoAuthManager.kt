package com.afternote.core.domain.repository.auth

import android.app.Activity

interface KakaoAuthManager {
    suspend fun login(activity: Activity): Result<String>
}
