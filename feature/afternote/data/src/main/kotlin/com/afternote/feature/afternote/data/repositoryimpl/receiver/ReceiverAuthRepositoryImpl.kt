package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.afternote.data.dto.DeliveryVerificationRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthCodeEmailSendRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyRequest
import com.afternote.feature.afternote.data.dto.ReceiverEmailAuthVerifyRequest
import com.afternote.feature.afternote.data.dto.toDomain
import com.afternote.feature.afternote.data.service.ReceiverAuthApiService
import com.afternote.feature.afternote.domain.error.ReceiverDeliverySubmitException
import com.afternote.feature.afternote.domain.error.ReceiverEmailAuthException
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `receiver-auth` 계열 endpoint 의 [ReceiverAuthRepository] 구현.
 *
 * 에러 처리 구조 — 일부 메서드는 try/catch 가 runCatching *안에* 포개져 있다:
 * 안쪽 catch 가 [ApiException](인프라 타입)을 도메인 예외로 바꿔 던지면(exception translation),
 * 그 새 예외는 자기를 만든 try 로 되돌아가지 않고 바깥 runCatching 이 잡아
 * `Result.failure(도메인 예외)` 로 반환된다 — 호출자에게 예외가 throw 되어 나가는 일은 없다.
 */
@Singleton
class ReceiverAuthRepositoryImpl
    @Inject
    constructor(
        private val api: ReceiverAuthApiService,
    ) : ReceiverAuthRepository {
        override suspend fun verify(authCode: String): Result<ReceiverIdentity> =
            runCatching {
                api.verify(ReceiverAuthVerifyRequest(authCode)).requireData().toDomain()
            }

        override suspend fun sendEmailAuthCode(email: String): Result<Unit> =
            runCatching {
                try {
                    api.sendEmailAuthCode(ReceiverAuthCodeEmailSendRequest(email)).requireStatus()
                } catch (e: ApiException) {
                    throw ReceiverEmailAuthException(serverMessage = e.serverMessage, serverCode = e.code)
                }
            }

        override suspend fun verifyEmailAuthCode(
            email: String,
            authCode: String,
        ): Result<ReceiverEmailAuthResult> =
            runCatching {
                try {
                    api
                        .verifyEmailAuthCode(
                            ReceiverEmailAuthVerifyRequest(email = email, authCode = authCode),
                        ).requireData()
                        .toDomain()
                } catch (e: ApiException) {
                    throw ReceiverEmailAuthException(serverMessage = e.serverMessage, serverCode = e.code)
                }
            }

        override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> =
            runCatching {
                api.getPresignedUrl(ReceiverAuthPresignedUrlRequest(extension)).requireData().toDomain()
            }

        override suspend fun submitDeliveryVerification(
            deathCertificateUrl: String?,
            familyRelationCertificateUrl: String?,
        ): Result<DeliveryVerification> =
            runCatching {
                try {
                    api
                        .submitDeliveryVerification(
                            DeliveryVerificationRequest(
                                deathCertificateUrl = deathCertificateUrl,
                                familyRelationCertificateUrl = familyRelationCertificateUrl,
                            ),
                        ).requireData()
                        .toDomain()
                } catch (e: ApiException) {
                    throw ReceiverDeliverySubmitException(serverMessage = e.serverMessage, httpCode = e.code)
                }
            }

        override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> =
            runCatching {
                api.getDeliveryVerificationStatus().requireData().toDomain()
            }

        override suspend fun getSenderMessage(): Result<SenderMessageInfo> =
            runCatching {
                api.getSenderMessage().requireData().toDomain()
            }
    }
