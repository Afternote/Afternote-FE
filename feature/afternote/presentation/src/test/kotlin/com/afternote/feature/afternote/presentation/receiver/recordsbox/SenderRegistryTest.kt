package com.afternote.feature.afternote.presentation.receiver.recordsbox

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenderRegistryTest {
    @Test
    fun `clear - 발신자와 인증 신원 상태를 모두 제거한다`() =
        runBlocking {
            val registry = SenderRegistry()
            val first = registry.register("첫 번째 발신자")
            val second = registry.register("두 번째 발신자")
            registry.attachIdentity(
                id = first.id,
                authCode = "AUTH-CODE-A",
                identity =
                    ReceiverIdentity(
                        receiverId = 1L,
                        receiverName = "수신자",
                        senderName = "실제 발신자",
                        relation = "친구",
                    ),
            )
            registry.updateVerificationStatus(first.id, DeliveryVerificationStatus.APPROVED)
            assertEquals("AUTH-CODE-A", registry.findById(first.id)?.authCode)
            assertEquals(DeliveryVerificationStatus.APPROVED, registry.findById(first.id)?.verificationStatus)
            assertEquals(2, registry.senders.value.size)

            registry.clear()

            assertTrue(registry.senders.value.isEmpty())
            assertNull(registry.findById(first.id))
            assertNull(registry.findById(second.id))
        }
}
