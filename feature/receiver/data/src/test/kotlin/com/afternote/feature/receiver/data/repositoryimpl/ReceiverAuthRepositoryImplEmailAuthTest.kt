package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.receiver.data.dto.DeliveryVerificationDto
import com.afternote.feature.receiver.data.dto.DeliveryVerificationRequestDto
import com.afternote.feature.receiver.data.dto.ReceivedRecordBoxListDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthCodeEmailSendRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthPresignedUrlDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthPresignedUrlRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthVerifyDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverEmailAuthVerifyDto
import com.afternote.feature.receiver.data.dto.ReceiverEmailAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverMessageDto
import com.afternote.feature.receiver.data.reporting.RecordingErrorReporter
import com.afternote.feature.receiver.data.service.ReceiverAuthApiService
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.error.ReceiverRejectionReason
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 이메일 인증 두 endpoint 의 `ApiException` → [ReceiverFailure.UserRejection] 도메인 예외 변환 회귀 가드 (#407).
 *
 * presentation 은 core:network 의 ApiException 을 직접 알면 안 되므로 (layer 규약)
 * Impl 이 서버 code 를 도메인 사유로 번역하는지가 계약. 에러 메시지·code 값은
 * 2026-06-11 라이브 서버 실응답 캡처 — 404 `{"code":1901,"message":"등록된 수신자 이메일이 아닙니다."}`,
 * 400 `{"code":1902,"message":"인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요."}`.
 *
 * 취소 전파도 여기서 함께 지킨다 (#671) — 예외를 Result 로 바꾸는 경계라 취소까지 삼키면
 * 취소된 코루틴에서 호출부의 onFailure 갈래가 돈다.
 */
class ReceiverAuthRepositoryImplEmailAuthTest {
    @Test
    fun `sendEmailAuthCode - 미등록 이메일 1901 ApiException 을 도메인 예외로 변환`() {
        val repository =
            ReceiverAuthRepositoryImpl(
                errorReporter = RecordingErrorReporter(),
                api =
                    FakeReceiverAuthApiService(
                        onSendEmailAuthCode = {
                            throw ApiException(
                                status = 404,
                                code = 1901,
                                serverMessage = "등록된 수신자 이메일이 아닙니다.",
                                fallbackMessage = "등록된 수신자 이메일이 아닙니다.",
                            )
                        },
                    ),
            )

        val result = runBlocking { repository.sendEmailAuthCode("none@example.com") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is ReceiverFailure.UserRejection)
        exception as ReceiverFailure.UserRejection
        assertEquals(ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND, exception.reason)
        assertTrue(exception.cause is ApiException)
    }

    @Test
    fun `verifyEmailAuthCode - 만료·미존재 1902 ApiException 을 도메인 예외로 변환`() {
        val repository =
            ReceiverAuthRepositoryImpl(
                errorReporter = RecordingErrorReporter(),
                api =
                    FakeReceiverAuthApiService(
                        onVerifyEmailAuthCode = {
                            throw ApiException(
                                status = 400,
                                code = 1902,
                                serverMessage = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                                fallbackMessage = "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.",
                            )
                        },
                    ),
            )

        val result = runBlocking { repository.verifyEmailAuthCode("a@b.com", "123456") }

        val exception = result.exceptionOrNull()
        assertTrue(exception is ReceiverFailure.UserRejection)
        exception as ReceiverFailure.UserRejection
        assertEquals(ReceiverRejectionReason.RECEIVER_EMAIL_AUTH_CODE_NOT_FOUND, exception.reason)
        assertTrue(exception.cause is ApiException)
    }

    /**
     * 서버에 닿지 못한 실패도 도메인 어휘로 나간다 — 형제인 목록 경로
     * ([com.afternote.feature.receiver.data.paging.ReceiverAfternotePagingSource]) 와 같은 계약이고,
     * [ReceiverFailure] 의 KDoc 이 «서버 응답과 네트워크 실패는 Data 계층이 이 계열로 번역한다» 로
     * 규정한 몫이다. 여기만 원본 [IOException] 을 흘리면 소비처의 «타입으로 안 갈린 것은 폴백» 규칙이
     * endpoint 마다 달라진다.
     */
    @Test
    fun `sendEmailAuthCode - 전송 계층 실패는 연결 불가 도메인 실패로 번역`() {
        val original = IOException("timeout")
        val repository =
            ReceiverAuthRepositoryImpl(
                errorReporter = RecordingErrorReporter(),
                api = FakeReceiverAuthApiService(onSendEmailAuthCode = { throw original }),
            )

        val result = runBlocking { repository.sendEmailAuthCode("a@b.com") }

        val exception = result.exceptionOrNull()
        assertTrue("$exception", exception is ReceiverFailure.NetworkUnavailable)
        assertEquals(original, exception?.cause)
    }

    /** 도메인 어휘가 없는 실패까지 번역하면 원인 타입이 소비처에서 사라진다. */
    @Test
    fun `sendEmailAuthCode - 분류 대상이 아닌 실패는 원본 그대로 전파`() {
        val original = IllegalStateException("boom")
        val repository =
            ReceiverAuthRepositoryImpl(
                api = FakeReceiverAuthApiService(onSendEmailAuthCode = { throw original }),
                errorReporter = RecordingErrorReporter(),
            )

        val result = runBlocking { repository.sendEmailAuthCode("a@b.com") }

        assertEquals(original, result.exceptionOrNull())
    }

    @Test
    fun `sendEmailAuthCode - in-flight 취소는 Result 로 삼키지 않고 CancellationException 을 전파`() =
        runBlocking {
            val repository =
                ReceiverAuthRepositoryImpl(
                    errorReporter = RecordingErrorReporter(),
                    api = FakeReceiverAuthApiService(onSendEmailAuthCode = { awaitCancellation() }),
                )

            var result: Result<Unit>? = null
            val job = launch { result = repository.sendEmailAuthCode("a@b.com") }
            yield() // job 이 api 호출 지점(awaitCancellation)까지 진행하도록
            job.cancel()
            job.join()

            assertNull(result) // 취소가 Result 로 둔갑했다면 non-null 로 남는다
        }

    @Test
    fun `verifyEmailAuthCode - 성공 응답을 도메인 모델로 매핑`() {
        val repository =
            ReceiverAuthRepositoryImpl(
                errorReporter = RecordingErrorReporter(),
                api =
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
                                    ),
                            )
                        },
                    ),
            )

        val result = runBlocking { repository.verifyEmailAuthCode("a@b.com", "123456") }.getOrThrow()

        assertEquals(3L, result.receiverId)
        assertEquals("큐에이수신자", result.receiverName)
        assertEquals("큐에이발신자", result.senderName)
    }
}

/** 이메일 인증 두 메서드만 주입 가능한 fake — 나머지 endpoint 는 본 테스트 대상 아님. */
private class FakeReceiverAuthApiService(
    private val onSendEmailAuthCode: suspend (ReceiverAuthCodeEmailSendRequestDto) -> BaseResponse<Unit> = { error("unused") },
    private val onVerifyEmailAuthCode: suspend (
        ReceiverEmailAuthVerifyRequestDto,
    ) -> BaseResponse<ReceiverEmailAuthVerifyDto> = { error("unused") },
) : ReceiverAuthApiService {
    override suspend fun verifyMasterKey(body: ReceiverAuthVerifyRequestDto): BaseResponse<ReceiverAuthVerifyDto> = error("unused")

    override suspend fun sendEmailAuthCode(body: ReceiverAuthCodeEmailSendRequestDto): BaseResponse<Unit> = onSendEmailAuthCode(body)

    override suspend fun verifyEmailAuthCode(body: ReceiverEmailAuthVerifyRequestDto): BaseResponse<ReceiverEmailAuthVerifyDto> =
        onVerifyEmailAuthCode(body)

    override suspend fun getPresignedUrl(body: ReceiverAuthPresignedUrlRequestDto): BaseResponse<ReceiverAuthPresignedUrlDto> =
        error("unused")

    override suspend fun submitDeliveryVerification(body: DeliveryVerificationRequestDto): BaseResponse<DeliveryVerificationDto> =
        error("unused")

    override suspend fun getDeliveryVerificationStatus(): BaseResponse<DeliveryVerificationDto> = error("unused")

    override suspend fun getSenderMessage(): BaseResponse<ReceiverMessageDto> = error("unused")

    override suspend fun getReceivedRecordBoxes(): BaseResponse<ReceivedRecordBoxListDto> = error("unused")
}
