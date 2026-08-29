package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.receiver.data.dto.DeliveryVerificationRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthCodeEmailSendRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthPresignedUrlRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverEmailAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.toDomain
import com.afternote.feature.receiver.data.error.toReceiverServerFailure
import com.afternote.feature.receiver.data.service.ReceiverAuthApiService
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
 * 에러 처리 구조 — 일부 메서드는 try/catch 가 [runCatchingCancellable] *안에* 포개져 있다:
 * 안쪽 catch 가 [ApiException](인프라 타입)을 도메인 예외로 바꿔 던지면(exception translation),
 * 그 새 예외는 자기를 만든 try 로 되돌아가지 않고 바깥 [runCatchingCancellable] 이 잡아
 * `Result.failure(도메인 예외)` 로 반환된다 — 호출자에게 예외가 throw 되어 나가는 일은 없다.
 * 안쪽 catch 는 [ApiException] 만 받으므로 취소는 여기 걸리지 않고 바깥 래퍼가 그대로 되던진다.
 */
@Singleton
class ReceiverAuthRepositoryImpl
    @Inject
    constructor(
        private val api: ReceiverAuthApiService,
    ) : ReceiverAuthRepository {
        override suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity> =
            runCatchingCancellable {
                try {
                    api.verifyMasterKey(ReceiverAuthVerifyRequestDto(authCode)).requireData().toDomain()
                } catch (e: ApiException) {
                    throw e.toReceiverServerFailure()
                }
            }

        override suspend fun sendEmailAuthCode(email: String): Result<Unit> =
            runCatchingCancellable {
                try {
                    api.sendEmailAuthCode(ReceiverAuthCodeEmailSendRequestDto(email)).requireStatus()
                } catch (e: ApiException) {
                    throw e.toReceiverServerFailure()
                }
            }

        override suspend fun verifyEmailAuthCode(
            email: String,
            authCode: String,
        ): Result<ReceiverEmailAuthResult> =
            runCatchingCancellable {
                try {
                    api
                        .verifyEmailAuthCode(
                            ReceiverEmailAuthVerifyRequestDto(email = email, authCode = authCode),
                        ).requireData()
                        .toDomain()
                } catch (e: ApiException) {
                    throw e.toReceiverServerFailure()
                }
            }

        override suspend fun getPresignedUrl(
            extension: String,
            contentLength: Long,
        ): Result<ReceiverAuthPresignedUrl> =
            runCatchingCancellable {
                api
                    .getPresignedUrl(
                        ReceiverAuthPresignedUrlRequestDto(
                            extension = extension,
                            contentLength = contentLength,
                        ),
                    ).requireData()
                    .toDomain()
            }

        override suspend fun submitDeliveryVerification(
            deathCertificateUrl: String?,
            familyRelationCertificateUrl: String?,
        ): Result<DeliveryVerification> =
            runCatchingCancellable {
                try {
                    api
                        .submitDeliveryVerification(
                            DeliveryVerificationRequestDto(
                                deathCertificateUrl = deathCertificateUrl,
                                familyRelationCertificateUrl = familyRelationCertificateUrl,
                            ),
                        ).requireData()
                        .toDomain()
                } catch (e: ApiException) {
                    throw e.toReceiverServerFailure()
                }
            }

        override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> =
            runCatchingCancellable {
                api.getDeliveryVerificationStatus().requireData().toDomain()
            }

        override suspend fun getSenderMessage(): Result<SenderMessageInfo> =
            runCatchingCancellable {
                api.getSenderMessage().requireData().toDomain()
            }
    }
