package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ImageApiService
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named

// DIRECTORY_AFTERNOTES 는 같은 패키지 [MemorialPhotoUploadRepositoryImpl] 의 internal const 공유.
private const val EXTENSION_JPG = "jpg"
private const val CONTENT_TYPE_JPEG = "image/jpeg"

/**
 * Uploads memorial thumbnail via POST /files/presigned-url (directory "afternotes") then S3 PUT.
 */
class MemorialThumbnailUploadRepositoryImpl
    @Inject
    constructor(
        private val imageApi: ImageApiService,
        @param:Named("S3Upload") private val okHttpClient: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MemorialThumbnailUploadRepository {
        override suspend fun uploadThumbnail(jpegBytes: ByteArray): Result<String> =
            runCatchingCancellable {
                val presigned =
                    imageApi
                        .getPresignedUrl(
                            PresignedUrlRequestDto(
                                directory = DIRECTORY_AFTERNOTES,
                                extension = EXTENSION_JPG,
                            ),
                        ).requireData()

                val contentType = presigned.contentType.ifBlank { CONTENT_TYPE_JPEG }
                val requestBody = jpegBytes.toRequestBody(contentType.toMediaType())
                val putRequest =
                    Request
                        .Builder()
                        .url(presigned.presignedUrl)
                        .put(requestBody)
                        .header("Content-Type", contentType)
                        .build()

                withContext(ioDispatcher) {
                    okHttpClient.newCall(putRequest).execute().use { response ->
                        check(response.isSuccessful) {
                            "S3 upload failed: ${response.code} ${response.message}"
                        }
                    }
                }
                presigned.fileUrl
            }
    }
