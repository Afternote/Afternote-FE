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
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * `receiver-auth` **모든** endpoint 가 실패를 도메인 어휘로 내보내는지의 회귀 가드(#1053).
 *
 * 형제 테스트인 [ReceiverAuthRepositoryImplEmailAuthTest] 가 이메일 인증 두 endpoint 의 사유 번역을
 * 지킨다면, 여기는 «번역이 붙지 않은 endpoint 가 남지 않는다» 를 지킨다. 메서드마다 `try/catch` 를
 * 포개던 시절에는 인증 계열 4개만 번역되고 상태 조회·기록함 목록·presigned URL 은 `ApiException`
 * (HTTP status·BE `ErrorCode` 번호·서버 문구)을 도메인 밖으로 그대로 흘렸다.
 *
 * 새 endpoint 가 늘면 [receiverAuthCalls] 에 한 줄을 더한다 — 목록에서 빠진 메서드는 이 가드가
 * 잡지 못하므로, 저장소 인터페이스의 메서드 수와 이 목록의 크기를 함께 단언한다.
 */
class ReceiverAuthRepositoryImplFailureTranslationTest {
    @Test
    fun `모든 endpoint 가 서버 거절을 도메인 사유로 옮긴다`() {
        val original =
            ApiException(
                status = 404,
                code = 1901,
                serverMessage = "등록된 수신자 이메일이 아닙니다.",
                fallbackMessage = "등록된 수신자 이메일이 아닙니다.",
            )
        val repository = repositoryFailingWith { throw original }

        receiverAuthCalls.forEach { (name, call) ->
            val exception = runBlocking { repository.call() }.exceptionOrNull()

            assertTrue("$name: $exception", exception is ReceiverFailure.UserRejection)
            exception as ReceiverFailure.UserRejection
            assertEquals(name, ReceiverRejectionReason.RECEIVER_EMAIL_NOT_FOUND, exception.reason)
            assertEquals(name, original, exception.cause)
        }
    }

    @Test
    fun `모든 endpoint 가 전송 계층 실패를 연결 불가로 옮긴다`() {
        val original = IOException("Unable to resolve host")
        val repository = repositoryFailingWith { throw original }

        receiverAuthCalls.forEach { (name, call) ->
            val exception = runBlocking { repository.call() }.exceptionOrNull()

            assertTrue("$name: $exception", exception is ReceiverFailure.NetworkUnavailable)
            assertEquals(name, original, exception?.cause)
        }
    }

    /**
     * 인프라 예외가 도메인 밖으로 새는지를 타입 이름으로 한 번 더 못 박는다 — 소비처(presentation)는
     * `core:network` 에 의존하지 않아 [ApiException] 을 받으면 사유를 가르지 못하고 폴백으로 수렴한다.
     */
    @Test
    fun `어떤 endpoint 도 ApiException 을 그대로 내보내지 않는다`() {
        val serverFailure = ApiException(status = 500, code = 500, serverMessage = null, fallbackMessage = "서버 오류")
        val repository = repositoryFailingWith { throw serverFailure }

        receiverAuthCalls.forEach { (name, call) ->
            val exception = runBlocking { repository.call() }.exceptionOrNull()

            assertTrue("$name: $exception", exception is ReceiverFailure.UnexpectedServerFailure)
            assertEquals(name, serverFailure, exception?.cause)
        }
    }

    /**
     * 목록이 저장소 인터페이스보다 뒤처지면 새 endpoint 는 번역 없이 지나간다 — 위 세 테스트가
     * 전부 통과해도 그 사실이 드러나지 않으므로, 메서드 이름을 인터페이스와 맞춘다.
     *
     * JVM 이름에서 접미사를 떼는 이유 — 반환 타입이 인라인 클래스([Result]) 라 컴파일러가
     * `verifyMasterKey-gIAlu-s` 처럼 시그니처 해시를 붙인다.
     */
    @Test
    fun `가드 목록이 저장소 메서드 전량을 덮는다`() {
        val declaredMethods =
            ReceiverAuthRepository::class.java.declaredMethods
                .map { it.name.substringBefore('-') }
                .toSet()

        assertEquals(declaredMethods, receiverAuthCalls.map { it.first }.toSet())
    }

    private fun repositoryFailingWith(failure: suspend () -> Nothing): ReceiverAuthRepository =
        ReceiverAuthRepositoryImpl(
            api = AlwaysFailingReceiverAuthApiService(failure),
            errorReporter = RecordingErrorReporter(),
        )
}

/**
 * 저장소가 서버를 부르는 메서드 전량. 인자 값은 어느 것이든 상관없다 — fake 가 호출 즉시 실패한다.
 */
private val receiverAuthCalls: List<Pair<String, suspend ReceiverAuthRepository.() -> Result<*>>> =
    listOf(
        "verifyMasterKey" to { verifyMasterKey("master-key") },
        "sendEmailAuthCode" to { sendEmailAuthCode("a@b.com") },
        "verifyEmailAuthCode" to { verifyEmailAuthCode(email = "a@b.com", authCode = "123456") },
        "getPresignedUrl" to { getPresignedUrl(extension = "pdf", contentLength = 10L) },
        "submitDeliveryVerification" to {
            submitDeliveryVerification(deathCertificateUrl = "https://example.com/a.pdf", familyRelationCertificateUrl = null)
        },
        "getDeliveryVerificationStatus" to { getDeliveryVerificationStatus() },
        "getSenderMessage" to { getSenderMessage() },
        "getReceivedRecordBoxes" to { getReceivedRecordBoxes() },
    )

/** 모든 endpoint 가 같은 실패를 던지는 fake — «번역이 붙지 않은 메서드» 를 드러내는 것이 목적이다. */
private class AlwaysFailingReceiverAuthApiService(
    private val failure: suspend () -> Nothing,
) : ReceiverAuthApiService {
    override suspend fun verifyMasterKey(body: ReceiverAuthVerifyRequestDto): BaseResponse<ReceiverAuthVerifyDto> = failure()

    override suspend fun sendEmailAuthCode(body: ReceiverAuthCodeEmailSendRequestDto): BaseResponse<Unit> = failure()

    override suspend fun verifyEmailAuthCode(body: ReceiverEmailAuthVerifyRequestDto): BaseResponse<ReceiverEmailAuthVerifyDto> = failure()

    override suspend fun getPresignedUrl(body: ReceiverAuthPresignedUrlRequestDto): BaseResponse<ReceiverAuthPresignedUrlDto> = failure()

    override suspend fun submitDeliveryVerification(body: DeliveryVerificationRequestDto): BaseResponse<DeliveryVerificationDto> = failure()

    override suspend fun getDeliveryVerificationStatus(): BaseResponse<DeliveryVerificationDto> = failure()

    override suspend fun getSenderMessage(): BaseResponse<ReceiverMessageDto> = failure()

    override suspend fun getReceivedRecordBoxes(): BaseResponse<ReceivedRecordBoxListDto> = failure()
}
