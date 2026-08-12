package com.afternote.core.data.repoimpl

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.PhotoUploadRepository
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

private const val DEFAULT_EXTENSION = "jpg"
private const val DEFAULT_CONTENT_TYPE = "image/jpeg"

private val ALLOWED_EXTENSIONS =
    setOf(
        "jpg",
        "jpeg",
        "png",
        "gif",
        "webp",
        "heic",
        "mp4",
        "mov",
        "mp3",
        "m4a",
        "wav",
        "pdf",
    )

private val MIME_TO_EXTENSION =
    mapOf(
        "image/heif" to "heic",
        "image/heic" to "heic",
        "image/jpg" to "jpg",
        "image/jfif" to "jpeg",
        "video/quicktime" to "mov",
        "audio/mpeg" to "mp3",
        "audio/mp4" to "m4a",
        "audio/x-m4a" to "m4a",
    )

private fun resolveExtension(mime: String?): String {
    if (mime == null) return DEFAULT_EXTENSION
    MIME_TO_EXTENSION[mime]
        ?.takeIf { it in ALLOWED_EXTENSIONS }
        ?.let { return it }
    val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
    val resolved =
        if (fromMime != null && fromMime in ALLOWED_EXTENSIONS) {
            fromMime
        } else {
            fromMime
        }
    return resolved?.takeIf { it in ALLOWED_EXTENSIONS } ?: DEFAULT_EXTENSION
}

class PhotoUploadRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val imageApi: ImageApiService,
        @param:Named("S3Upload") private val s3Client: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : PhotoUploadRepository {
        override suspend fun upload(
            uriString: String,
            directory: String,
        ): Result<String> =
            runCatchingCancellable {
                val uri = uriString.toUri()
                val mime = context.contentResolver.getType(uri)
                val extension = resolveExtension(mime)
                android.util.Log.d("PhotoUpload", "mime=$mime → extension=$extension")

                val presigned =
                    imageApi
                        .getPresignedUrl(
                            PresignedUrlRequestDto(
                                directory = directory,
                                extension = extension,
                            ),
                        ).requireData()

                val tempFile =
                    withContext(ioDispatcher) {
                        val file = File.createTempFile("photo_upload_", ".$extension", context.cacheDir)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw IllegalStateException("Could not read image from URI")
                        file
                    }

                try {
                    val contentType = presigned.contentType.ifBlank { DEFAULT_CONTENT_TYPE }
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
                                    "S3 upload failed: ${response.code} ${response.message}"
                                }
                            }
                    }

                    presigned.fileUrl
                } finally {
                    tempFile.delete()
                }
            }
    }
