package com.afternote.core.network.service

import com.afternote.core.network.dto.AppVersionCheckResponseDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 앱 버전 확인 API.
 * GET /api/v1/app/version — 현재 설치 버전과 서버 최신 버전을 비교해 강제 업데이트 필요 여부를 반환한다.
 */
interface AppVersionApiService {
    @GET("app/version")
    suspend fun checkVersion(
        @Query("platform") platform: String,
        @Query("versionCode") versionCode: Int,
    ): BaseResponse<AppVersionCheckResponseDto>
}
