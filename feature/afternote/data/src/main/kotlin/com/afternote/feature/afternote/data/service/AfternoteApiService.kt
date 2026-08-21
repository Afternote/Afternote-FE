package com.afternote.feature.afternote.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteIdDto
import com.afternote.feature.afternote.data.dto.AfternotePageDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AfternoteApiService {
    @GET("afternotes")
    suspend fun getAfternotes(
        @Query("category") category: String?,
        @Query("page") pageNumber: Int?,
        @Query("size") size: Int?,
    ): BaseResponse<AfternotePageDto>

    @GET("afternotes/{afternoteId}")
    suspend fun getAfternoteDetail(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<AfternoteDetailDto>

    /** SOCIAL·BUSINESS 공용 생성 — 두 카테고리는 바디 스키마가 동일해 [AfternoteCreateAccountRequestDto.type]으로만 구분된다. */
    @POST("afternotes")
    suspend fun createAfternoteAccount(
        @Body request: AfternoteCreateAccountRequestDto,
    ): BaseResponse<AfternoteIdDto>

    @POST("afternotes")
    suspend fun createAfternoteGallery(
        @Body request: AfternoteCreateGalleryRequestDto,
    ): BaseResponse<AfternoteIdDto>

    @POST("afternotes")
    suspend fun createAfternotePlaylist(
        @Body request: AfternoteCreatePlaylistRequestDto,
    ): BaseResponse<AfternoteIdDto>

    @PATCH("afternotes/{afternoteId}")
    suspend fun updateAfternote(
        @Path("afternoteId") afternoteId: Long,
        @Body request: AfternoteUpdateRequestDto,
    ): BaseResponse<AfternoteIdDto>

    @DELETE("afternotes/{afternoteId}")
    suspend fun deleteAfternote(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<Unit>
}
