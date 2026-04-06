package com.afternote.feature.afternote.data.service

import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreateSocialRequest
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
    @GET("api/afternotes")
    suspend fun getAfternotes(
        @Query("category") category: String? = null,
        @Query("pageNumber") pageNumber: Int = 0,
        @Query("size") size: Int = 10,
    ): BaseResponse<AfternoteListResponse>

    @GET("api/afternotes/{afternoteId}")
    suspend fun getAfternoteDetail(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<AfternoteDetailResponse>

    @POST("api/afternotes")
    suspend fun createAfternoteSocial(
        @Body request: AfternoteCreateSocialRequest,
    ): BaseResponse<AfternoteIdResponse>

    @POST("api/afternotes")
    suspend fun createAfternoteGallery(
        @Body request: AfternoteCreateGalleryRequest,
    ): BaseResponse<AfternoteIdResponse>

    @POST("api/afternotes")
    suspend fun createAfternotePlaylist(
        @Body request: AfternoteCreatePlaylistRequest,
    ): BaseResponse<AfternoteIdResponse>

    @PATCH("api/afternotes/{afternoteId}")
    suspend fun updateAfternote(
        @Path("afternoteId") afternoteId: Long,
        @Body request: AfternoteUpdateRequest,
    ): BaseResponse<AfternoteIdResponse>

    @DELETE("api/afternotes/{afternoteId}")
    suspend fun deleteAfternote(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<AfternoteIdResponse>
}
