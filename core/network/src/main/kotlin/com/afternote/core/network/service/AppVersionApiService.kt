package com.afternote.core.network.service

import com.afternote.core.network.dto.AppPlatformDto
import com.afternote.core.network.dto.AppVersionDto
import com.afternote.core.network.model.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** 인증 없이 현재 Android 빌드의 강제 업데이트 필요 여부를 확인하는 API. */
interface AppVersionApiService {
    @GET("app/version")
    suspend fun checkVersion(
        @Query("platform") platform: AppPlatformDto,
        @Query("versionCode") versionCode: Int,
    ): BaseResponse<AppVersionDto>
}
