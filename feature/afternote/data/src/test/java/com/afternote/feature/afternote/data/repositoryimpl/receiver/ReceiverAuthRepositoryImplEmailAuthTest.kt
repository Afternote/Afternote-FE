package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.afternote.data.dto.DeliveryVerificationDto
import com.afternote.feature.afternote.data.dto.DeliveryVerificationRequestDto
import com.afternote.feature.afternote.data.dto.ReceiverAuthCodeEmailSendRequestDto
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlDto
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlRequestDto
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyDto
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyRequestDto
import com.afternote.feature.afternote.data.dto.ReceiverEmailAuthVerifyDto
import com.afternote.feature.afternote.data.dto.ReceiverEmailAuthVerifyRequestDto
import com.afternote.feature.afternote.data.dto.ReceiverMessageDto
import com.afternote.feature.afternote.data.service.ReceiverAuthApiService
import com.afternote.feature.afternote.domain.error.ReceiverEmailAuthException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 이메일 인증 두 endpoint 의 `ApiException` → [ReceiverEmailAuthException] 도메인 예외 변환 회귀 가드 (#407).
 *
 * presentation 은 core:network 의 ApiException 을 직접 알면 안 되므로 (layer 규약)
 * Impl 이 serverMessage·code 를 보존해 변환하는지가 계약. 에러 메시지·code 값은
 * 2026-06-11 라이브 서버 실응답 캡처 — 404 `{"code":1901,"message":"등록된 수신자 이메일이 아닙니다."}`,
 * 400 `{"code":1902,"message":"인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요."}`.
 */
class ReceiverAuthRepositoryImplEmailAuthTest {
    @Test
    fun `sendEmailAuthCode - 미등록 이메일 1901 ApiException 을 도메인 예외로 변환`() {
        val repository =
            ReceiverAuthRepositoryImpl(
                FakeReceiverAuthApiService(
                    onSendEmailAuthCode = {
                        throw ApiException(
                            code = 1901,
                            serverMessage = "등록된 수신자 이메일이 아닙니다.",
                            message = "등록된 수신자 이메일이 아닙니다.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.sendEmailAuthCode("none@example.com") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is ReceiverEmailAuthException)
        exception as ReceiverEmailAuthException
        assertEquals("등록된 수신자 이메일이 아닙니다.", exception.serverMessage)
        assertEquals(1901, exception.serverCode)
    }

    @Test
    fun `verifyEmailAuthCode - 만료·불일치 1902 ApiException 을 도메인 예외로 변환`() {
        val repository =
            ReceiverAuthRepositoryImpl(
                FakeReceiverAuthApiService(
                    onVerifyEmailAuthCode = {
                        throw ApiException(
                            code = 1902,
                            serverMessage = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                            message = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                        )
                    },
                ),
            )

        val result = runBlocking { repository.verifyEmailAuthCode("a@b.com", "123456") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is ReceiverEmailAuthException)
        exception as ReceiverEmailAuthException
        assertEquals("인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.", exception.serverMessage)
        assertEquals(1902, exception.serverCode)
    }

    @Test
    fun `sendEmailAuthCode - ApiException 아닌 인프라 예외는 원본 그대로 전파`() {
        val original = IOException("timeout")
        val repository =
            ReceiverAuthRepositoryImpl(
                FakeReceiverAuthApiService(onSendEmailAuthCode = { throw original }),
            )

        val result = runBlocking { repository.sendEmailAuthCode("a@b.com") }

        assertEquals(original, result.exceptionOrNull())
    }

    @Test
    fun `verifyEmailAuthCode - 성공 응답을 도메인 모델로 매핑`() {
        val repository =
            ReceiverAuthRepositoryImpl(
                FakeReceiverAuthApiService(
                    onVerifyEmailAuthCode = { body ->
                        assertEquals("a@b.com", body.email)
                        assertEquals("123456", body.authCode)
                        BaseResponse(
                            status = 200,
                            code = 200,
                            message = "성공",
                            data =
                                ReceiverEmailAuthVerifyDto(
                                    receiverId = 3L,
                                    receiverName = "큐에이수신자",
                                    senderName = "큐에이발신자",
                                    accessCode = "123e4567-e89b-12d3-a456-426614174000",
                                ),
                        )
                    },
                ),
            )

        val result = runBlocking { repository.verifyEmailAuthCode("a@b.com", "123456") }.getOrThrow()

        assertEquals(3L, result.receiverId)
        assertEquals("123e4567-e89b-12d3-a456-426614174000", result.accessCode)
    }
}

/** 이메일 인증 두 메서드만 주입 가능한 fake — 나머지 endpoint 는 본 테스트 대상 아님. */
private class FakeReceiverAuthApiService(
    private val onSendEmailAuthCode: (ReceiverAuthCodeEmailSendRequestDto) -> BaseResponse<Unit> = { error("unused") },
    private val onVerifyEmailAuthCode: (
        ReceiverEmailAuthVerifyRequestDto,
    ) -> BaseResponse<ReceiverEmailAuthVerifyDto> = { error("unused") },
) : ReceiverAuthApiService {
    override suspend fun verify(body: ReceiverAuthVerifyRequestDto): BaseResponse<ReceiverAuthVerifyDto> = error("unused")

    override suspend fun sendEmailAuthCode(body: ReceiverAuthCodeEmailSendRequestDto): BaseResponse<Unit> = onSendEmailAuthCode(body)

    override suspend fun verifyEmailAuthCode(body: ReceiverEmailAuthVerifyRequestDto): BaseResponse<ReceiverEmailAuthVerifyDto> =
        onVerifyEmailAuthCode(body)

    override suspend fun getPresignedUrl(body: ReceiverAuthPresignedUrlRequestDto): BaseResponse<ReceiverAuthPresignedUrlDto> =
        error("unused")

    override suspend fun submitDeliveryVerification(body: DeliveryVerificationRequestDto): BaseResponse<DeliveryVerificationDto> =
        error("unused")

    override suspend fun getDeliveryVerificationStatus(): BaseResponse<DeliveryVerificationDto> = error("unused")

    override suspend fun getSenderMessage(): BaseResponse<ReceiverMessageDto> = error("unused")
}
