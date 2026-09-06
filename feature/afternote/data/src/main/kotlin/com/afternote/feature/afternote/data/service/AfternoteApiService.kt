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
    /**
     * 서버는 [draftOnly] 미전송을 `false` 로 읽어 **발행 완료만** 준다. 임시저장 목록은 `true` 로 따로 받는다 —
     * 한 요청에 섞어 주는 모드가 없다(BE `AfternoteService.getAfternotes`).
     */
    @GET("afternotes")
    suspend fun getAfternotes(
        @Query("category") category: String?,
        @Query("page") pageNumber: Int?,
        @Query("size") size: Int?,
        @Query("draftOnly") draftOnly: Boolean? = null,
    ): BaseResponse<AfternotePageDto>

    @GET("afternotes/{afternoteId}")
    suspend fun getAfternoteDetail(
        @Path("afternoteId") afternoteId: Long,
    ): BaseResponse<AfternoteDetailDto>

    /** SOCIAL·BUSINESS 공용 생성 — 두 카테고리는 바디 스키마가 동일해 [AfternoteCreateAccountRequestDto.category] 로만 구분된다. */
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
