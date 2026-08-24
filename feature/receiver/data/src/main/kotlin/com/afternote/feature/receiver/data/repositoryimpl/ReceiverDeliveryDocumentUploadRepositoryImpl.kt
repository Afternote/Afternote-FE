package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named

/**
 * `getPresignedUrl(extension)` 으로 presigned URL 을 받아 바이트를 PUT 하는 일반화된 구현.
 *
 * 인증·헤더 부착은 [ReceiverAuthRepository] 측 호출에서, S3 PUT 은 [okHttpClient] 로 분리되어
 * 작성자 측 [com.afternote.feature.afternote.data.repositoryimpl.author.MemorialThumbnailUploadRepositoryImpl]
 * 와 동일한 패턴을 따른다.
 */
class ReceiverDeliveryDocumentUploadRepositoryImpl
    @Inject
    constructor(
        private val receiverAuthRepository: ReceiverAuthRepository,
        @param:Named("S3Upload") private val okHttpClient: OkHttpClient,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ReceiverDeliveryDocumentUploadRepository {
        override suspend fun upload(
            bytes: ByteArray,
            extension: String,
        ): Result<String> =
            runCatchingCancellable {
                val presigned =
                    receiverAuthRepository
                        .getPresignedUrl(extension)
                        .getOrThrow()

                val contentType = presigned.contentType.ifBlank { CONTENT_TYPE_FALLBACK }
                val body = bytes.toRequestBody(contentType.toMediaType())
                val request =
                    Request
                        .Builder()
                        .url(presigned.presignedUrl)
                        .put(body)
                        .header("Content-Type", contentType)
                        .build()

                withContext(ioDispatcher) {
                    okHttpClient.newCall(request).execute().use { response ->
                        check(response.isSuccessful) {
                            "S3 upload failed: ${response.code} ${response.message}"
                        }
                    }
                }
                presigned.fileUrl
            }

        private companion object {
            const val CONTENT_TYPE_FALLBACK = "application/octet-stream"
        }
    }
