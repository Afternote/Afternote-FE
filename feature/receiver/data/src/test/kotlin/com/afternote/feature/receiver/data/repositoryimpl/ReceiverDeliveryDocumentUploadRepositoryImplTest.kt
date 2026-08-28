package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverDeliveryDocumentUploadRepositoryImplTest {
    @Test
    fun `업로드 바이트 크기를 presigned 요청과 S3 Content-Length에 동일하게 쓴다`() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val authRepository =
            RecordingReceiverAuthRepository(
                presigned =
                    ReceiverAuthPresignedUrl(
                        presignedUrl = "https://s3.example/document.pdf",
                        fileKey = "receiver-auth/staging/document.pdf",
                        fileUrl = "https://cdn.example/document.pdf",
                        contentType = "application/pdf",
                        contentLength = bytes.size.toLong(),
                    ),
            )
        var uploadedRequest: Request? = null
        val uploadClient =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    uploadedRequest = chain.request()
                    successfulResponse(chain.request())
                }.build()
        val repository =
            ReceiverDeliveryDocumentUploadRepositoryImpl(
                receiverAuthRepository = authRepository,
                okHttpClient = uploadClient,
                ioDispatcher = Dispatchers.Unconfined,
            )

        val uploadedUrl = runBlocking { repository.upload(bytes, "pdf").getOrThrow() }

        assertEquals("pdf", authRepository.requestedExtension)
        assertEquals(bytes.size.toLong(), authRepository.requestedContentLength)
        val request = requireNotNull(uploadedRequest)
        assertEquals(bytes.size.toString(), request.header("Content-Length"))
        assertEquals("application/pdf", request.header("Content-Type"))
        val requestBody = requireNotNull(request.body)
        assertEquals(bytes.size.toLong(), requestBody.contentLength())
        val sink = Buffer()
        requestBody.writeTo(sink)
        assertArrayEquals(bytes, sink.readByteArray())
        assertEquals("https://cdn.example/document.pdf", uploadedUrl)
    }

    @Test
    fun `서버가 다른 contentLength를 돌려주면 S3 PUT 전에 실패한다`() {
        val bytes = byteArrayOf(1, 2, 3)
        val authRepository =
            RecordingReceiverAuthRepository(
                presigned =
                    ReceiverAuthPresignedUrl(
                        presignedUrl = "https://s3.example/document.pdf",
                        fileKey = "receiver-auth/staging/document.pdf",
                        fileUrl = "https://cdn.example/document.pdf",
                        contentType = "application/pdf",
                        contentLength = bytes.size.toLong() + 1,
                    ),
            )
        var uploadCallCount = 0
        val uploadClient =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    uploadCallCount += 1
                    successfulResponse(chain.request())
                }.build()
        val repository =
            ReceiverDeliveryDocumentUploadRepositoryImpl(
                receiverAuthRepository = authRepository,
                okHttpClient = uploadClient,
                ioDispatcher = Dispatchers.Unconfined,
            )

        val result = runBlocking { repository.upload(bytes, "pdf") }

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("content length mismatch"))
        assertEquals(0, uploadCallCount)
    }

    private fun successfulResponse(request: Request): Response =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ByteArray(0).toResponseBody())
            .build()
}

private class RecordingReceiverAuthRepository(
    private val presigned: ReceiverAuthPresignedUrl,
) : ReceiverAuthRepository {
    var requestedExtension: String? = null
        private set
    var requestedContentLength: Long? = null
        private set

    override suspend fun getPresignedUrl(
        extension: String,
        contentLength: Long,
    ): Result<ReceiverAuthPresignedUrl> {
        requestedExtension = extension
        requestedContentLength = contentLength
        return Result.success(presigned)
    }

    override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> = error("unused")

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> = error("unused")

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> = error("unused")

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> = error("unused")

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> = error("unused")

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> = error("unused")
}
