package com.afternote.feature.receiver.domain.testing

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeReceiverRepositoriesTest {
    @Test
    fun `인증 코드 저장은 호출 원문을 기록하고 정규화한 상태를 flow와 단발 조회에 공유한다`() {
        val repository = FakeReceiverRepository(initialMasterKey = "  initial  ")

        assertEquals("initial", repository.masterKeyState.value)
        assertNull(FakeReceiverRepository(initialMasterKey = "   ").masterKeyState.value)

        runBlocking { repository.saveMasterKey("  next-code  ") }

        assertEquals(listOf("  next-code  "), repository.savedMasterKeys)
        assertEquals("next-code", repository.masterKeyState.value)
        assertEquals("next-code", runBlocking { repository.currentMasterKey() })
        assertSame(repository.masterKeyState, repository.masterKeyFlow)

        runBlocking { repository.saveMasterKey("   ") }

        assertNull(repository.masterKeyState.value)
    }

    @Test
    fun `ReceiverRepository onX는 기록 뒤 기본 메모리 변경을 대체한다`() {
        val repository =
            FakeReceiverRepository(
                initialMasterKey = "initial",
                onSaveMasterKey = {},
            )

        runBlocking { repository.saveMasterKey("replacement") }

        assertEquals(listOf("replacement"), repository.savedMasterKeys)
        assertEquals("initial", repository.masterKeyState.value)
    }

    @Test
    fun `인증 제출 기본 경로는 URL을 기록하고 이후 상태 조회에도 보존한다`() {
        val repository = FakeReceiverAuthRepository()

        val submitted =
            runBlocking {
                repository.submitDeliveryVerification(
                    deathCertificateUrl = "https://cdn.test/death.pdf",
                    familyRelationCertificateUrl = null,
                )
            }.getOrThrow()
        val reloaded = runBlocking { repository.getDeliveryVerificationStatus() }.getOrThrow()

        assertEquals(
            listOf("https://cdn.test/death.pdf" to null),
            repository.deliverySubmissions,
        )
        assertEquals("https://cdn.test/death.pdf", submitted.deathCertificateUrl)
        assertNull(submitted.familyRelationCertificateUrl)
        assertEquals(submitted, reloaded)
        assertEquals(1, repository.getDeliveryVerificationStatusCalls)
    }

    @Test
    fun `presigned URL 기본 경로는 요청 contentLength를 응답에도 보존한다`() {
        val repository = FakeReceiverAuthRepository()

        val result = runBlocking { repository.getPresignedUrl("pdf", 357L) }.getOrThrow()

        assertEquals(listOf("pdf" to 357L), repository.presignedUrlRequests)
        assertEquals(357L, result.contentLength)
    }

    @Test
    fun `서류 업로드는 바이트 snapshot과 확장자를 기록한다`() {
        val repository =
            FakeReceiverDeliveryDocumentUploadRepository(
                defaultFileUrl = "https://cdn.test/document.pdf",
            )
        val bytes = byteArrayOf(1, 2, 3)

        val result = runBlocking { repository.upload(bytes, "pdf") }
        bytes[0] = 9

        assertEquals("https://cdn.test/document.pdf", result.getOrThrow())
        assertEquals(1, repository.uploadCalls.size)
        assertTrue(
            repository.uploadCalls
                .single()
                .bytes
                .contentEquals(byteArrayOf(1, 2, 3)),
        )
        assertEquals("pdf", repository.uploadCalls.single().extension)
    }

    @Test
    fun `본인 확인 기본 경로는 호출을 기록하고 해당 발신자만 true로 바꾼다`() {
        val repository = FakeIdentityVerificationRepository()

        runBlocking { repository.markVerified("sender-a") }

        assertEquals(1, repository.markVerifiedCallCount)
        assertEquals(listOf("sender-a"), repository.markVerifiedSenderIds)
        assertEquals(setOf("sender-a"), repository.verifiedSenderIds.value)
        assertTrue(runBlocking { repository.isVerified("sender-a").first() })
        assertFalse(runBlocking { repository.isVerified("sender-b").first() })
        assertTrue(FakeIdentityVerificationRepository().verifiedSenderIds.value.isEmpty())
    }

    @Test
    fun `병렬 호출도 업로드 기록과 본인 확인 호출 수를 유실하지 않는다`() {
        val uploadRepository = FakeReceiverDeliveryDocumentUploadRepository()
        val identityRepository = FakeIdentityVerificationRepository()

        runBlocking {
            coroutineScope {
                repeat(64) { index ->
                    launch(Dispatchers.Default) {
                        uploadRepository.upload(byteArrayOf(index.toByte()), "pdf").getOrThrow()
                        identityRepository.markVerified("sender-$index")
                    }
                }
            }
        }

        assertEquals(64, uploadRepository.uploadCalls.size)
        assertEquals(64, identityRepository.markVerifiedCallCount)
        assertEquals(64, identityRepository.verifiedSenderIds.value.size)
    }

    @Test
    fun `strict fixture는 flow 프로퍼티를 포함해 열지 않은 경로를 실패시킨다`() {
        val receiverRepository = FakeReceiverRepository.strict()
        val authRepository = FakeReceiverAuthRepository.strict()

        assertThrows(IllegalStateException::class.java) {
            receiverRepository.masterKeyFlow
        }
        assertThrows(IllegalStateException::class.java) {
            runBlocking { authRepository.getSenderMessage() }
        }
    }

    @Test
    fun `SenderRegistry strict fixture의 flow는 안전하고 suspend 호출은 failure Result를 반환한다`() {
        val initial = SenderEntry(id = "sender-1", name = "별칭")
        val repository = FakeSenderRegistryRepository.strict(initialSenders = listOf(initial))
        val identity = ReceiverIdentity(1L, "수신자", "발신자", "친구")

        assertEquals(listOf(initial), runBlocking { repository.senders.first() })
        assertTrue(runBlocking { repository.register("새 별칭") }.isFailure)
        assertTrue(runBlocking { repository.findById(initial.id) }.isFailure)
        assertTrue(runBlocking { repository.attachIdentity(initial.id, "key", identity) }.isFailure)
        assertTrue(
            runBlocking {
                repository.updateVerificationStatus(initial.id, DeliveryVerificationStatus.APPROVED)
            }.isFailure,
        )
        assertEquals(listOf(initial), repository.senderEntries.value)
    }
}
