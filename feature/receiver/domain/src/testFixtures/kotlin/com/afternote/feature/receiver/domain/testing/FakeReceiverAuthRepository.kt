package com.afternote.feature.receiver.domain.testing

import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceivedRecordBox
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [ReceiverAuthRepository] fake 정본 (#1030, #1042).
 *
 * 기본은 인증 응답과 제출 상태를 메모리에 보관하고 모든 호출을 기록한다. 특정 실패나
 * 경합처럼 저장소 상태만으로 표현할 수 없는 시나리오는 `onX` 로 갈아끼운다.
 */
class FakeReceiverAuthRepository(
    var identity: ReceiverIdentity = DEFAULT_IDENTITY,
    var emailAuthResult: ReceiverEmailAuthResult = DEFAULT_EMAIL_AUTH_RESULT,
    var presignedUrl: ReceiverAuthPresignedUrl = DEFAULT_PRESIGNED_URL,
    var deliveryVerification: DeliveryVerification = DEFAULT_DELIVERY_VERIFICATION,
    var senderMessage: SenderMessageInfo = DEFAULT_SENDER_MESSAGE,
    var recordBoxes: List<ReceivedRecordBox> = emptyList(),
    var onVerifyMasterKey: (suspend (String) -> Result<ReceiverIdentity>)? = null,
    var onSendEmailAuthCode: (suspend (String) -> Result<Unit>)? = null,
    var onVerifyEmailAuthCode: (suspend (String, String) -> Result<ReceiverEmailAuthResult>)? = null,
    var onGetPresignedUrl: (suspend (String, Long) -> Result<ReceiverAuthPresignedUrl>)? = null,
    var onSubmitDeliveryVerification: (suspend (String?, String?) -> Result<DeliveryVerification>)? = null,
    var onGetDeliveryVerificationStatus: (suspend () -> Result<DeliveryVerification>)? = null,
    var onGetSenderMessage: (suspend () -> Result<SenderMessageInfo>)? = null,
    var onGetReceivedRecordBoxes: (suspend () -> Result<List<ReceivedRecordBox>>)? = null,
) : ReceiverAuthRepository {
    val verifiedMasterKeys = CopyOnWriteArrayList<String>()
    val sentEmails = CopyOnWriteArrayList<String>()
    val verifiedEmailCodes = CopyOnWriteArrayList<Pair<String, String>>()
    val presignedUrlRequests = CopyOnWriteArrayList<Pair<String, Long>>()
    val deliverySubmissions = CopyOnWriteArrayList<Pair<String?, String?>>()

    private val deliveryVerificationStatusCounter = AtomicInteger()
    private val senderMessageCounter = AtomicInteger()
    private val recordBoxesCounter = AtomicInteger()

    val getDeliveryVerificationStatusCalls: Int
        get() = deliveryVerificationStatusCounter.get()

    val getSenderMessageCalls: Int
        get() = senderMessageCounter.get()

    val getReceivedRecordBoxesCalls: Int
        get() = recordBoxesCounter.get()

    override suspend fun verifyMasterKey(masterKey: String): Result<ReceiverIdentity> {
        verifiedMasterKeys += masterKey
        onVerifyMasterKey?.let { return it(masterKey) }
        return Result.success(identity)
    }

    override suspend fun sendEmailAuthCode(email: String): Result<Unit> {
        sentEmails += email
        onSendEmailAuthCode?.let { return it(email) }
        return Result.success(Unit)
    }

    override suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult> {
        verifiedEmailCodes += email to authCode
        onVerifyEmailAuthCode?.let { return it(email, authCode) }
        return Result.success(emailAuthResult)
    }

    override suspend fun getPresignedUrl(
        extension: String,
        contentLength: Long,
    ): Result<ReceiverAuthPresignedUrl> {
        presignedUrlRequests += extension to contentLength
        onGetPresignedUrl?.let { return it(extension, contentLength) }
        return Result.success(presignedUrl.copy(contentLength = contentLength))
    }

    override suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification> {
        deliverySubmissions += deathCertificateUrl to familyRelationCertificateUrl
        onSubmitDeliveryVerification?.let {
            return it(deathCertificateUrl, familyRelationCertificateUrl)
        }
        deliveryVerification =
            deliveryVerification.copy(
                status = DeliveryVerificationStatus.PENDING,
                deathCertificateUrl = deathCertificateUrl,
                familyRelationCertificateUrl = familyRelationCertificateUrl,
            )
        return Result.success(deliveryVerification)
    }

    override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> {
        deliveryVerificationStatusCounter.incrementAndGet()
        onGetDeliveryVerificationStatus?.let { return it() }
        return Result.success(deliveryVerification)
    }

    override suspend fun getSenderMessage(): Result<SenderMessageInfo> {
        senderMessageCounter.incrementAndGet()
        onGetSenderMessage?.let { return it() }
        return Result.success(senderMessage)
    }

    override suspend fun getReceivedRecordBoxes(): Result<List<ReceivedRecordBox>> {
        recordBoxesCounter.incrementAndGet()
        onGetReceivedRecordBoxes?.let { return it() }
        return Result.success(recordBoxes)
    }

    companion object {
        private val DEFAULT_IDENTITY =
            ReceiverIdentity(
                receiverId = 1L,
                receiverName = "수신자",
                senderName = "발신자",
                relation = "가족",
            )
        private val DEFAULT_EMAIL_AUTH_RESULT =
            ReceiverEmailAuthResult(
                receiverId = 1L,
                receiverName = "수신자",
                senderName = "발신자",
            )
        private val DEFAULT_PRESIGNED_URL =
            ReceiverAuthPresignedUrl(
                presignedUrl = "https://upload.afternote.test/document",
                fileKey = "receiver/document.pdf",
                fileUrl = "https://cdn.afternote.test/receiver/document.pdf",
                contentType = "application/pdf",
                contentLength = 0L,
            )
        private val DEFAULT_DELIVERY_VERIFICATION =
            DeliveryVerification(
                id = 1L,
                status = DeliveryVerificationStatus.PENDING,
                deathCertificateUrl = null,
                familyRelationCertificateUrl = null,
                adminNote = null,
                createdAt = null,
            )
        private val DEFAULT_SENDER_MESSAGE =
            SenderMessageInfo(
                senderName = "발신자",
                message = null,
                createdAt = null,
            )

        /** 모든 경로를 닫고, 테스트가 쓰는 `onX` 만 명시적으로 연다. */
        fun strict(): FakeReceiverAuthRepository =
            FakeReceiverAuthRepository(
                onVerifyMasterKey = { unexpectedCall("ReceiverAuthRepository.verifyMasterKey") },
                onSendEmailAuthCode = { unexpectedCall("ReceiverAuthRepository.sendEmailAuthCode") },
                onVerifyEmailAuthCode = { _, _ -> unexpectedCall("ReceiverAuthRepository.verifyEmailAuthCode") },
                onGetPresignedUrl = { _, _ -> unexpectedCall("ReceiverAuthRepository.getPresignedUrl") },
                onSubmitDeliveryVerification = { _, _ ->
                    unexpectedCall("ReceiverAuthRepository.submitDeliveryVerification")
                },
                onGetDeliveryVerificationStatus = {
                    unexpectedCall("ReceiverAuthRepository.getDeliveryVerificationStatus")
                },
                onGetSenderMessage = { unexpectedCall("ReceiverAuthRepository.getSenderMessage") },
                onGetReceivedRecordBoxes = { unexpectedCall("ReceiverAuthRepository.getReceivedRecordBoxes") },
            )
    }
}
