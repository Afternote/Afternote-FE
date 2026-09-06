package com.afternote.feature.afternote.data.repositoryimpl.author

import android.content.Context
import androidx.core.net.toUri
import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.dto.PresignedUrlRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ImageApiService
import com.afternote.feature.afternote.domain.repository.author.MemorialAudioFormats
import com.afternote.feature.afternote.domain.repository.author.MemorialAudioUploadRepository
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

/**
 * 추모 음성 업로드 (#1118). presigned 발급 → S3 PUT 흐름은 사진·영상과 같고, 다른 것은 **확장자 결정**뿐이다.
 *
 * 못 읽은 MIME 을 기본값으로 폴백하지 않고 실패로 끝낸다 — 서버가
 * `PlaylistValidationStrategy` 에서 확장자를 다시 검사하므로, 폴백해 올려 봐야 저장이 400 으로 끝나고
 * S3 에는 참조 없는 객체만 남는다. 지원 형식 판정은 [MemorialAudioFormats] 하나가 갖는다.
 */
class MemorialAudioUploadRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val imageApi: ImageApiService,
        @param:Named("S3Upload") private val s3Client: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MemorialAudioUploadRepository {
        override suspend fun upload(uriString: String): Result<String> =
            runCatchingCancellable {
                val uri = uriString.toUri()
                val mimeType = context.contentResolver.getType(uri)
                val extension =
                    MemorialAudioFormats.extensionFor(mimeType)
                        ?: throw IllegalArgumentException("unsupported memorial audio type: $mimeType")

                // presigned 요청에 파일 크기가 필수라 임시 파일을 먼저 만든다 (#950).
                val tempFile =
                    withContext(ioDispatcher) {
                        val file = File.createTempFile("audio_upload_", ".$extension", context.cacheDir)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        } ?: throw IllegalStateException("Could not read audio from URI")
                        file
                    }

                val presigned =
                    try {
                        imageApi
                            .getPresignedUrl(
                                PresignedUrlRequestDto(
                                    directory = DIRECTORY_AFTERNOTES,
                                    extension = extension,
                                    contentLength = tempFile.length(),
                                ),
                            ).requireData()
                    } catch (e: Throwable) {
                        tempFile.delete()
                        throw e
                    }

                try {
                    val contentType = presigned.contentType.ifBlank { mimeType.orEmpty().ifBlank { "audio/$extension" } }
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
                                    "S3 audio upload failed: ${response.code} ${response.message}"
                                }
                            }
                    }

                    presigned.fileUrl
                } finally {
                    tempFile.delete()
                }
            }
    }
