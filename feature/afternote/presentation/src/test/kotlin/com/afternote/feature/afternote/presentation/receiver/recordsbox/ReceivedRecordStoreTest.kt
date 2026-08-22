package com.afternote.feature.afternote.presentation.receiver.recordsbox

import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ReceivedRecordStoreTest {
    @Test
    fun `항목 조회 - 식별자가 일치하는 항목 하나만 반환`() {
        val store = ReceivedRecordStore()
        val entry = recordBoxEntry(recordBoxId = 1L)
        store.replaceRecordBoxes(listOf(entry))

        assertEquals(entry, store.findByRecordBoxId(recordBoxId = 1L))
        assertNull(store.findByRecordBoxId(recordBoxId = 2L))
    }

    @Test
    fun `목록 교체 - recordBoxId가 중복되면 거부하고 기존 스냅샷 유지`() {
        val store = ReceivedRecordStore()
        val existingEntry = recordBoxEntry(recordBoxId = 1L)
        store.replaceRecordBoxes(listOf(existingEntry))

        val exception =
            assertThrows(DuplicateRecordBoxIdException::class.java) {
                store.replaceRecordBoxes(
                    listOf(
                        recordBoxEntry(recordBoxId = 2L),
                        recordBoxEntry(recordBoxId = 2L),
                    ),
                )
            }

        assertEquals(2L, exception.recordBoxId)
        assertEquals(listOf(existingEntry), store.recordBoxes.value)
        assertEquals(existingEntry, store.findByRecordBoxId(recordBoxId = 1L))
    }
}

private fun recordBoxEntry(recordBoxId: Long): ReceivedRecordItem =
    ReceivedRecordItem(
        recordBoxId = recordBoxId,
        accessCode = "record-key-$recordBoxId",
        senderName = "발신자 $recordBoxId",
        receiverName = "수신자",
        relation = "OTHER",
        recordStatus = ReceivedRecordStatus.Stored,
        viewStatus = ReceivedRecordViewStatus.Requestable,
        verification = ReceivedRecordVerification.NotRequested,
    )
