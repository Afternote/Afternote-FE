package com.afternote.core.data.repositoryImpl

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ImageApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.cancellation.CancellationException

private const val DEFAULT_EXTENSION = "jpg"
private const val DEFAULT_CONTENT_TYPE = "image/jpeg"

class PhotoUploadRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val imageApi: ImageApiService,
        @Named("IoDispatcher") private val ioDispatcher: CoroutineDispatcher,
    ) : PhotoUploadRepository {
        override suspend fun upload(
            uriString: String,
            directory: String,
        ): Result<String> =
            try {
                val uri = uriString.toUri()
                val mime = context.contentResolver.getType(uri)
                val extension =
                    mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                        ?: DEFAULT_EXTENSION

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

                    val response = imageApi.uploadToS3(
                        url = presigned.presignedUrl,
                        file = requestBody,
                        contentType = contentType,
                    )
                    check(response.isSuccessful) {
                        "S3 upload failed: ${response.code()} ${response.message()}"
                    }

                    Result.success(presigned.fileUrl)
                } finally {
                    tempFile.delete()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
    }
