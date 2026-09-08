package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.core.common.di.IoDispatcher
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.receiver.data.error.mapReceiverFailure
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
 * 파일 크기와 확장자로 presigned URL 을 받아 같은 크기의 바이트를 PUT 하는 일반화된 구현.
 *
 * 인증·헤더 부착은 [ReceiverAuthRepository] 측 호출에서, S3 PUT 은 [okHttpClient] 로 분리되어
 * 작성자 측 [com.afternote.feature.afternote.data.repositoryimpl.author.MemorialThumbnailUploadRepositoryImpl]
 * 와 동일한 패턴을 따른다.
 *
 * 실패도 이 모듈의 다른 저장소와 같은 규약으로 내보낸다 — presigned URL 발급 실패는 이미 번역돼
 * 오고, S3 PUT 이 전송 계층에서 끝난 경우는 [com.afternote.feature.receiver.data.error.mapReceiverFailure]
 * 가 [com.afternote.feature.receiver.domain.error.ReceiverFailure.NetworkUnavailable] 로 옮긴다.
 * 크기 불일치·비정상 응답 코드처럼 도메인 어휘가 없는 실패는 원본 그대로 나간다.
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
                        .getPresignedUrl(
                            extension = extension,
                            contentLength = bytes.size.toLong(),
                        ).getOrThrow()

                check(presigned.contentLength == bytes.size.toLong()) {
                    "Presigned content length mismatch: expected ${bytes.size}, " +
                        "received ${presigned.contentLength}"
                }
                val contentType = presigned.contentType.ifBlank { CONTENT_TYPE_FALLBACK }
                val body = bytes.toRequestBody(contentType.toMediaType())
                val request =
                    Request
                        .Builder()
                        .url(presigned.presignedUrl)
                        .put(body)
                        .header("Content-Type", contentType)
                        .header("Content-Length", presigned.contentLength.toString())
                        .build()

                withContext(ioDispatcher) {
                    okHttpClient.newCall(request).execute().use { response ->
                        check(response.isSuccessful) {
                            "S3 upload failed: ${response.code} ${response.message}"
                        }
                    }
                }
                presigned.fileUrl
            }.mapReceiverFailure()

        private companion object {
            const val CONTENT_TYPE_FALLBACK = "application/octet-stream"
        }
    }
