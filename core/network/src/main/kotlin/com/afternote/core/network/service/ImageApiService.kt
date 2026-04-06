package com.afternote.core.network.service

import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.dto.PresignedUrlResponseDto
import com.afternote.core.network.model.BaseResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Url

/**
 * File API (presigned URL for S3 upload).
 * POST /files/presigned-url — S3 파일 업로드를 위한 Presigned URL을 생성합니다.
 * 지원 형식: 이미지(jpg, jpeg, png, gif, webp, heic), 영상(mp4, mov), 음성(mp3, m4a, wav), 문서(pdf).
 */
interface ImageApiService {
    @POST("files/presigned-url")
    suspend fun getPresignedUrl(
        @Body body: PresignedUrlRequestDto,
    ): BaseResponse<PresignedUrlResponseDto>

    @PUT
    suspend fun uploadToS3(
        @Url url: String,
        @Body file: RequestBody,
        @Header("Content-Type") contentType: String,
    ): Response<Unit>
}
