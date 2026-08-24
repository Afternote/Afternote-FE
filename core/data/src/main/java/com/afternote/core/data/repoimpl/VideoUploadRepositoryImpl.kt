package com.afternote.core.data.repoimpl

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.VideoUploadRepository
import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ImageApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Named

/** MIME 타입에서 확장자를 못 뽑았을 때 폴백. 대부분의 안드로이드 영상이 mp4 라 합리적 디폴트. */
private const val DEFAULT_VIDEO_EXTENSION = "mp4"

internal class VideoUploadRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val imageApi: ImageApiService,
        @param:Named("S3Upload") private val s3Client: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : VideoUploadRepository {
        override suspend fun upload(
            uriString: String,
            directory: String,
        ): Result<String> =
            runCatchingCancellable {
                val uri = uriString.toUri()
                val extension = videoExtensionFromUri(uri)

                // presigned 요청에 파일 크기가 필수라 임시 파일을 먼저 만든다 (#950).
                val tempFile =
                    withContext(ioDispatcher) {
                        val file = File.createTempFile("video_upload_", ".$extension", context.cacheDir)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw IllegalStateException("Could not read video from URI")
                        file
                    }

                val presigned =
                    try {
                        imageApi
                            .getPresignedUrl(
                                PresignedUrlRequestDto(
                                    directory = directory,
                                    extension = extension,
                                    contentLength = tempFile.length(),
                                ),
                            ).requireData()
                    } catch (e: Throwable) {
                        tempFile.delete()
                        throw e
                    }

                try {
                    val contentType = presigned.contentType.ifBlank { "video/$extension" }
                    val requestBody = tempFile.asRequestBody(contentType.toMediaType())

                    withContext(ioDispatcher) {
                        s3Client
                            .newCall(
                                Request
                                    .Builder()
                                    .url(presigned.presignedUrl)
                                    .put(requestBody)
                                    .header("Content-Type", contentType)
                                    .build(),
                            ).execute()
                            .use { response ->
                                check(response.isSuccessful) {
                                    "S3 video upload failed: ${response.code} ${response.message}"
                                }
                            }
                    }

                    presigned.fileUrl
                } finally {
                    tempFile.delete()
                }
            }

        private fun videoExtensionFromUri(uri: Uri): String {
            val mime = context.contentResolver.getType(uri) ?: return DEFAULT_VIDEO_EXTENSION
            return when {
                mime == "video/mp4" -> {
                    "mp4"
                }

                mime == "video/quicktime" -> {
                    "mov"
                }

                mime.startsWith("video/") -> {
                    mime.removePrefix("video/").takeIf { it.isNotBlank() }
                        ?: DEFAULT_VIDEO_EXTENSION
                }

                else -> {
                    DEFAULT_VIDEO_EXTENSION
                }
            }
        }
    }
