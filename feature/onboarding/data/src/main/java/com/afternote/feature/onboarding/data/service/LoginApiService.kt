package com.afternote.feature.onboarding.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.onboarding.data.dto.request.KakaoLoginRequest
import com.afternote.feature.onboarding.data.dto.response.LoginTokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginApiService {
    /**
     * POST /auth/kakao — 카카오 액세스 토큰을 서버에 전달해 앱 토큰을 발급받습니다.
     */
    @POST("/auth/kakao")
    suspend fun loginWithKakao(
        @Body request: KakaoLoginRequest,
    ): BaseResponse<LoginTokenResponse>
}
