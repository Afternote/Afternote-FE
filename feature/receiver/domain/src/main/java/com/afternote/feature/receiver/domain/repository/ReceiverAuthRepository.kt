package com.afternote.feature.receiver.domain.repository

import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo

/**
 * 수신자 인증 흐름 전용 Repository (`receiver-auth/...` 경로).
 *
 * 기존 `ReceiverRepository` 는 인증된 수신자가 사용하는 일반 조회 (after-notes 등) 책임.
 * 본 Repository 는 인증 자체 + 서류 제출/검증 + sender 메시지 조회를 담당한다.
 *
 * 인증 코드 저장은 `ReceiverRepository.saveAuthCode` 를 사용하며,
 * 저장된 코드는 `ReceiverAuthInterceptor` 가 `X-Auth-Code` 헤더로 자동 부착한다.
 * (둘 다 feature:afternote 쪽이라 이 모듈에서 KDoc 링크로 참조할 수 없다 — 의존 방향이 반대.)
 */
interface ReceiverAuthRepository {
    /**
     * 발신자가 발급한 마스터 키([authCode])로 수신자 신원을 확인한다.
     *
     * 성공하면 이 키가 이후 요청의 `X-Auth-Code` 헤더로 재사용된다 (사실상 수신자 세션).
     * 서버 거절은 `ReceiverFailure.UserRejection` 또는 `ReceiverFailure.UnexpectedServerFailure` 로 번역된다.
     */
    suspend fun verifyMasterKey(authCode: String): Result<ReceiverIdentity>

    /**
     * 수신자 본인 확인 — 수신자 레코드에 등록된 [email] 로 6자리 인증번호 발송.
     *
     * 발신자가 수신자 등록 시 email 을 넣지 않았으면 서버가 거절한다 (RECEIVER_EMAIL_NOT_FOUND).
     * 서버 거절은 Data 계층에서 도메인 사유로 번역되며, 표시 문구는 presentation 리소스가 갖는다.
     */
    suspend fun sendEmailAuthCode(email: String): Result<Unit>

    /**
     * 수신자 본인 확인 — [email] 로 발송된 6자리 [authCode] 검증.
     *
     * 실패(만료/불일치 등)는 `ReceiverFailure.UserRejection` 과 구체 사유로 번역된다.
     */
    suspend fun verifyEmailAuthCode(
        email: String,
        authCode: String,
    ): Result<ReceiverEmailAuthResult>

    /**
     * [contentLength] 바이트를 업로드할 presigned URL을 발급한다.
     *
     * 서버는 이 값을 요청 필수 필드로 받고, 응답에 같은 값을 돌려준다. S3 PUT의
     * `Content-Length`도 이 값과 일치해야 한다.
     */
    suspend fun getPresignedUrl(
        extension: String,
        contentLength: Long,
    ): Result<ReceiverAuthPresignedUrl>

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
