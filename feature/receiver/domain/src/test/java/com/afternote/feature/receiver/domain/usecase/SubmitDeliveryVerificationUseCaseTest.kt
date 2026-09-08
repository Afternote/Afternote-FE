package com.afternote.feature.receiver.domain.usecase

import com.afternote.feature.receiver.domain.error.DeliveryDocumentsMissingException
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SubmitDeliveryVerificationUseCase] 가 소유하는 제출 불변식 (#380, #1701).
 *
 * 계약 — 사망확인서·가족관계증명서 URL 이 둘 다 없으면 Repository 를 부르지 않고
 * [DeliveryDocumentsMissingException] 으로 닫는다. 한쪽이라도 있으면 두 값을 그대로 전달한다.
 */
class SubmitDeliveryVerificationUseCaseTest {
    @Test
    fun `두 URL이 모두 없으면 Repository를 부르지 않고 서류 누락으로 닫는다`() {
        // strict fake 라 호출되면 unexpectedCall 로도 터진다 — 기록 단언과 이중으로 막는다.
        val repository = FakeReceiverAuthRepository.strict()

        val result =
            runBlocking {
                SubmitDeliveryVerificationUseCase(repository)(
                    deathCertificateUrl = null,
                    familyRelationCertificateUrl = null,
                )
            }

        assertTrue(repository.deliverySubmissions.isEmpty())
        assertTrue(result.exceptionOrNull() is DeliveryDocumentsMissingException)
    }

    @Test
    fun `사망확인서만 있으면 가족관계증명서 자리를 비운 채 전달한다`() {
        val repository = FakeReceiverAuthRepository()

        val result =
            runBlocking {
                SubmitDeliveryVerificationUseCase(repository)(
                    deathCertificateUrl = "https://cdn.test/death.pdf",
                    familyRelationCertificateUrl = null,
                )
            }

        assertEquals(listOf("https://cdn.test/death.pdf" to null), repository.deliverySubmissions)
        assertEquals("https://cdn.test/death.pdf", result.getOrThrow().deathCertificateUrl)
    }

    @Test
    fun `가족관계증명서만 있어도 제출된다`() {
        val repository = FakeReceiverAuthRepository()

        val result =
            runBlocking {
                SubmitDeliveryVerificationUseCase(repository)(
                    deathCertificateUrl = null,
                    familyRelationCertificateUrl = "https://cdn.test/family.pdf",
                )
            }

        assertEquals(listOf(null to "https://cdn.test/family.pdf"), repository.deliverySubmissions)
        assertEquals("https://cdn.test/family.pdf", result.getOrThrow().familyRelationCertificateUrl)
    }

    @Test
    fun `두 URL이 모두 있으면 둘 다 실려 나간다`() {
        val repository = FakeReceiverAuthRepository()

        val result =
            runBlocking {
                SubmitDeliveryVerificationUseCase(repository)(
                    deathCertificateUrl = "https://cdn.test/death.pdf",
                    familyRelationCertificateUrl = "https://cdn.test/family.pdf",
                )
            }

        assertEquals(
            listOf("https://cdn.test/death.pdf" to "https://cdn.test/family.pdf"),
            repository.deliverySubmissions,
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Repository 실패는 그대로 흘려보낸다`() {
        val failure = IllegalStateException("서버 거절")
        val repository =
            FakeReceiverAuthRepository.strict().apply {
                onSubmitDeliveryVerification = { _, _ -> Result.failure(failure) }
            }

        val result =
            runBlocking {
                SubmitDeliveryVerificationUseCase(repository)(
                    deathCertificateUrl = "https://cdn.test/death.pdf",
                    familyRelationCertificateUrl = null,
                )
            }

        assertEquals(failure, result.exceptionOrNull())
    }
}
