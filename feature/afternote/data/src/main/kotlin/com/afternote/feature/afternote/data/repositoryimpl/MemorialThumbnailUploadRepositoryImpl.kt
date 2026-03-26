package com.afternote.feature.afternote.data.repositoryimpl

import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ImageApiService
import com.afternote.feature.afternote.domain.repository.MemorialThumbnailUploadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named

private const val DIRECTORY_AFTERNOTES = "afternotes"
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
        @param:Named("IoDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : MemorialThumbnailUploadRepository {
        override suspend fun uploadThumbnail(jpegBytes: ByteArray): Result<String> =
            runCatching {
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
