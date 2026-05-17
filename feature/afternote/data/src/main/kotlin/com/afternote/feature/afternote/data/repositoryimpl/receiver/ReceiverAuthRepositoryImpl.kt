package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.data.dto.DeliveryVerificationRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthPresignedUrlRequest
import com.afternote.feature.afternote.data.dto.ReceiverAuthVerifyRequest
import com.afternote.feature.afternote.data.dto.toDomain
import com.afternote.feature.afternote.data.service.ReceiverAuthApiService
import com.afternote.feature.afternote.domain.model.receiver.DeliveryVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceiverAuthPresignedUrl
import com.afternote.feature.afternote.domain.model.receiver.ReceiverIdentity
import com.afternote.feature.afternote.domain.model.receiver.SenderMessageInfo
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverAuthRepository
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
