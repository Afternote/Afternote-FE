package com.afternote.feature.afternote.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequest
import com.afternote.feature.afternote.data.dto.AfternoteDetailResponse
import com.afternote.feature.afternote.data.dto.AfternoteIdResponse
import com.afternote.feature.afternote.data.dto.AfternoteListResponse
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequest
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
    ): BaseResponse<AfternoteListResponse>

    @GET("afternotes/{afternoteId}")
    suspend fun getAfternoteDetail(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<AfternoteDetailResponse>

    /** SOCIAL·BUSINESS 공용 생성 — 두 카테고리는 바디 스키마가 동일해 [AfternoteCreateAccountRequest.category] 로만 구분된다. */
    @POST("afternotes")
    suspend fun createAfternoteAccount(
        @Body request: AfternoteCreateAccountRequest,
    ): BaseResponse<AfternoteIdResponse>

    @POST("afternotes")
    suspend fun createAfternoteGallery(
        @Body request: AfternoteCreateGalleryRequest,
    ): BaseResponse<AfternoteIdResponse>

    @POST("afternotes")
    suspend fun createAfternotePlaylist(
        @Body request: AfternoteCreatePlaylistRequest,
    ): BaseResponse<AfternoteIdResponse>

    @PATCH("afternotes/{afternoteId}")
    suspend fun updateAfternote(
        @Path("afternoteId") afternoteId: Long,
        @Body request: AfternoteUpdateRequest,
    ): BaseResponse<AfternoteIdResponse>

    @DELETE("afternotes/{afternoteId}")
    suspend fun deleteAfternote(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<Unit>
}
