package com.afternote.feature.afternote.domain.repository.receiver

import com.afternote.feature.afternote.domain.model.receiver.DeliveryVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceiverAuthPresignedUrl
import com.afternote.feature.afternote.domain.model.receiver.ReceiverIdentity
import com.afternote.feature.afternote.domain.model.receiver.SenderMessageInfo

/**
 * 수신자 인증 흐름 전용 Repository (`receiver-auth/...` 경로).
 *
 * 기존 [ReceiverRepository] 는 인증된 수신자가 사용하는 일반 조회 (after-notes 등) 책임.
 * 본 Repository 는 인증 자체 + 서류 제출/검증 + sender 메시지 조회를 담당한다.
 *
 * 인증 코드 저장은 [ReceiverRepository.saveAuthCode] 를 사용하며,
 * 저장된 코드는 [com.afternote.feature.afternote.data.network.ReceiverAuthInterceptor] 가
 * `X-Auth-Code` 헤더로 자동 부착한다.
 */
interface ReceiverAuthRepository {
    suspend fun verify(authCode: String): Result<ReceiverIdentity>

    suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl>

    suspend fun submitDeliveryVerification(
        deathCertificateUrl: String,
        familyRelationCertificateUrl: String,
    ): Result<DeliveryVerification>

    suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification>

    suspend fun getSenderMessage(): Result<SenderMessageInfo>
}
