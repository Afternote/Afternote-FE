package com.afternote.feature.receiver.domain.repository

import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo

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

    /**
     * 수신자 본인 확인 — 수신자 레코드에 등록된 [email] 로 6자리 인증번호 발송.
     *
     * 발신자가 수신자 등록 시 email 을 넣지 않았으면 서버가 거절한다 (RECEIVER_EMAIL_NOT_FOUND).
     * 실패는 `ReceiverEmailAuthException` 으로 매핑되어 serverMessage 에 안내 문구가 담긴다.
     */
    suspend fun sendEmailAuthCode(email: String): Result<Unit>

    /**
     * 수신자 본인 확인 — [email] 로 발송된 6자리 [authCode] 검증.
     *
     * 실패(만료/불일치 등)는 `ReceiverEmailAuthException` 으로 매핑된다.
     */
    suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult>

    suspend fun getPresignedUrl(extension: String): Result<ReceiverAuthPresignedUrl>

    /**
     * 사망확인 서류 제출 — 두 URL 중 **하나 이상** 은 non-null 이어야 한다 (서버가 최소 1개 요구).
     * 둘 다 null 인 호출은 서버가 거절하므로 호출부(ViewModel)가 사전 차단한다.
     */
    suspend fun submitDeliveryVerification(
        deathCertificateUrl: String?,
        familyRelationCertificateUrl: String?,
    ): Result<DeliveryVerification>

    suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification>

    suspend fun getSenderMessage(): Result<SenderMessageInfo>
}
