package com.afternote.feature.receiver.domain.usecase

import com.afternote.feature.receiver.domain.error.DeliveryDocumentsMissingException
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.repository.ReceiverAuthRepository
import javax.inject.Inject

/**
 * 열람 신청 서류 제출 (#215, #380, #1701).
 *
 * 불변식 — 사망확인서·가족관계증명서 URL 중 **하나 이상**이 있어야 제출이 성립한다. 이 규칙은
 * 서버 계약이지 화면 사정이 아니라서 ViewModel 이 아니라 여기가 소유한다. 둘 다 없으면
 * Repository 를 부르지 않고 [DeliveryDocumentsMissingException] 으로 닫는다 — 어차피 서버가
 * 거절할 요청이라 왕복시킬 이유가 없고, 화면은 «업로드부터 하라» 는 안내를 그 실패로 고른다.
 *
 * 빈 문자열을 «없음» 으로 정규화하지 않는다. URL 은 업로드 성공 응답에서만 채워지므로 빈 값이
 * 도달할 경로가 없고, 여기서 정규화하면 서버로 나가는 페이로드가 조용히 달라진다.
 */
class SubmitDeliveryVerificationUseCase
    @Inject
    constructor(
        private val receiverAuthRepository: ReceiverAuthRepository,
    ) {
        suspend operator fun invoke(
            deathCertificateUrl: String?,
            familyRelationCertificateUrl: String?,
        ): Result<DeliveryVerification> {
            if (deathCertificateUrl == null && familyRelationCertificateUrl == null) {
                return Result.failure(DeliveryDocumentsMissingException())
            }
            return receiverAuthRepository.submitDeliveryVerification(
                deathCertificateUrl = deathCertificateUrl,
                familyRelationCertificateUrl = familyRelationCertificateUrl,
            )
        }
    }
