package com.afternote.feature.afternote.data.repositoryimpl.author

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ImageApiService
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
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

/** 서버 S3 의 애프터노트 미디어 폴더명. presigned URL 경로에 박혀 `bucket/afternotes/<file>` 형태가 됨. 서버와 약속된 문자열. */
private const val DIRECTORY_AFTERNOTES = "afternotes"

/** MIME 타입에서 확장자를 못 뽑았을 때 폴백. 대부분의 안드로이드 영상이 mp4 라 합리적 디폴트. */
private const val DEFAULT_VIDEO_EXTENSION = "mp4"

/** Android 의 *로컬 파일 URI* 스킴. 갤러리/카메라 picker 결과는 `content://...` 로 시작 → "아직 서버에 없음, 업로드 필요" 의 신호. `https://...` 같은 원격 URL 과 구분하는 용도. */
private const val LOCAL_CONTENT_SCHEME = "content://"

/**
 * 추모 영상 *상태 해석* + 필요 시 업로드.
 *
 * 입력 String 의 형식을 판별해 sealed 분기로 반환:
 * - 로컬 `content://` URI → presigned PUT 으로 S3 업로드 후 [VideoUploadOutcome.FreshlyUploaded]
 * - 원격 HTTPS URL → 입력 그대로 [VideoUploadOutcome.Existing]
 * - null/blank → [VideoUploadOutcome.Empty]
 *
 * `content://` prefix 비교는 *data 레이어 안* 에 격리 — 도메인은 인프라 형식 디테일을 모름.
 */
class MemorialVideoUploadRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val imageApi: ImageApiService,
        @param:Named("S3Upload") private val okHttpClient: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MemorialVideoUploadRepository {
        override suspend fun resolveVideo(input: String?): Result<VideoUploadOutcome> {
            if (input.isNullOrBlank()) return Result.success(VideoUploadOutcome.Empty)
            if (!input.startsWith(LOCAL_CONTENT_SCHEME)) {
                return Result.success(VideoUploadOutcome.Existing(input))
            }
            return uploadLocalVideo(input).map { VideoUploadOutcome.FreshlyUploaded(it) }
        }

        private suspend fun uploadLocalVideo(contentUriString: String): Result<String> =
            runCatching {
                val uri = contentUriString.toUri()
                val extension = videoExtensionFromUri(uri)
                val presigned =
                    imageApi
                        .getPresignedUrl(
                            PresignedUrlRequestDto(
                                directory = DIRECTORY_AFTERNOTES,
                                extension = extension,
                            ),
                        ).requireData()

                val tempFile =
                    withContext(ioDispatcher) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File.createTempFile("memorial_video_", ".$extension")
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                            file
                        } ?: throw IllegalStateException("Could not read video from URI")
                    }

                try {
                    val contentType = presigned.contentType.ifBlank { "video/$extension" }
                    val requestBody = tempFile.asRequestBody(contentType.toMediaType())
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
                                "S3 video upload failed: ${response.code} ${response.message}"
                            }
                        }
                    }
                    presigned.fileUrl
                } finally {
                    if (!tempFile.delete()) {
                        // Temp file cleanup failed; file may remain until system cleanup
                    }
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
