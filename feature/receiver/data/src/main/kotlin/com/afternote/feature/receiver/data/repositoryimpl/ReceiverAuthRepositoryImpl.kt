package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.receiver.data.dto.DeliveryVerificationRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthCodeEmailSendRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthPresignedUrlRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.ReceiverEmailAuthVerifyRequestDto
import com.afternote.feature.receiver.data.dto.toDomain
import com.afternote.feature.receiver.data.error.mapReceiverFailure
import com.afternote.feature.receiver.data.service.ReceiverAuthApiService
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.ReceivedRecordBox
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
 * 에러 처리 구조 — 모든 메서드가 `runCatchingCancellable { ... }.mapReceiverFailure()` 한 형태다.
 * [com.afternote.core.network.model.ApiException] 이 나르는 HTTP status·BE `ErrorCode` 번호·서버 문구는
 * [com.afternote.feature.receiver.data.error.mapReceiverFailure] 안에서만 해석되고, 호출자에게는
 * [com.afternote.feature.receiver.domain.error.ReceiverFailure] 의 도메인 어휘만 나간다.
 *
 * 메서드별 `try/catch` 대신 공통 꼬리를 쓰는 이유 — 번역을 붙인 endpoint 와 빠뜨린 endpoint 가
 * 섞이면 같은 저장소가 어떤 호출에서는 도메인 어휘를, 어떤 호출에서는 인프라 예외를 내보낸다.
 * 소비처는 그 차이를 알 수 없어 «타입으로 갈리지 않으면 폴백» 규칙이 조용히 무너진다.
 */
@Singleton
class ReceiverAuthRepositoryImpl
    @Inject
    constructor(
        private val api: ReceiverAuthApiService,
        private val errorReporter: ErrorReporter,
    ) : ReceiverAuthRepository {
        override suspend fun verifyMasterKey(masterKey: String): Result<ReceiverIdentity> =
            runCatchingCancellable {
                api.verifyMasterKey(ReceiverAuthVerifyRequestDto(masterKey)).requireData().toDomain()
            }.mapReceiverFailure()

        override suspend fun sendEmailAuthCode(email: String): Result<Unit> =
            runCatchingCancellable {
                api.sendEmailAuthCode(ReceiverAuthCodeEmailSendRequestDto(email)).requireStatus()
            }.mapReceiverFailure()

        override suspend fun verifyEmailAuthCode(
            email: String,
            authCode: String,
        ): Result<ReceiverEmailAuthResult> =
            runCatchingCancellable {
                api
                    .verifyEmailAuthCode(
                        ReceiverEmailAuthVerifyRequestDto(email = email, authCode = authCode),
                    ).requireData()
                    .toDomain()
            }.mapReceiverFailure()

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
            }.mapReceiverFailure()

        override suspend fun submitDeliveryVerification(
            deathCertificateUrl: String?,
            familyRelationCertificateUrl: String?,
        ): Result<DeliveryVerification> =
            runCatchingCancellable {
                api
                    .submitDeliveryVerification(
                        DeliveryVerificationRequestDto(
                            deathCertificateUrl = deathCertificateUrl,
                            familyRelationCertificateUrl = familyRelationCertificateUrl,
                        ),
                    ).requireData()
                    .toDomain(errorReporter)
            }.mapReceiverFailure()

        override suspend fun getDeliveryVerificationStatus(): Result<DeliveryVerification> =
            runCatchingCancellable {
                api.getDeliveryVerificationStatus().requireData().toDomain(errorReporter)
            }.mapReceiverFailure()

        override suspend fun getSenderMessage(): Result<SenderMessageInfo> =
            runCatchingCancellable {
                api.getSenderMessage().requireData().toDomain()
            }.mapReceiverFailure()

        override suspend fun getReceivedRecordBoxes(): Result<List<ReceivedRecordBox>> =
            runCatchingCancellable {
                api
                    .getReceivedRecordBoxes()
                    .requireData()
                    .toDomain(errorReporter)
            }.mapReceiverFailure()
    }
