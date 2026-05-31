package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.data.dto.DeliveryVerificationRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyRequest
import com.afternote.feature.afternote.data.dto.toDomain
import com.afternote.feature.afternote.data.service.ReceiverAuthApiService
import com.afternote.feature.afternote.domain.error.ReceiverDeliverySubmitException
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

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

        override suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl> =
            runCatching {
                api.getPresignedUrl(ReceiverAuthPresignedUrlRequest(extension)).requireData().toDomain()
            }

        override suspend fun submitDeliveryVerification(
            deathCertificateUrl: String,
            familyRelationCertificateUrl: String,
        ): Result<DeliveryVerification> =
            runCatching {
                api
                    .submitDeliveryVerification(
                        DeliveryVerificationRequest(
                            deathCertificateUrl = deathCertificateUrl,
                            familyRelationCertificateUrl = familyRelationCertificateUrl,
                        ),
                    ).requireData()
                    .toDomain()
            }.recoverCatching { throwable ->
                // ApiErrorInterceptor 가 4xx/5xx 응답을 ApiException 으로 변환 (백엔드 message 포함).
                // presentation 이 core:network 의 ApiException 을 직접 알면 layer 위반이므로
                // 도메인 예외 ReceiverDeliverySubmitException 으로 변환해 노출한다.
                //
                // ApiException.serverMessage = 서버가 실제 보낸 message (null 가능),
                // ApiException.message = 클라 fallback 섞여 있어 사용자 노출 부적합 — serverMessage 만 전달.
                throw if (throwable is ApiException) {
                    ReceiverDeliverySubmitException(
                        serverMessage = throwable.serverMessage,
                        httpCode = throwable.code,
                    )
                } else {
                    throwable
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
