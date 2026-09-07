package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.core.model.user.Receiver
import com.afternote.feature.afternote.presentation.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiverListState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorReceiverIdContractTest {
    @Test
    fun `등록 수신자의 Long id 를 문자열 변환 없이 에디터 모델로 옮긴다`() {
        val receiver = Receiver(Long.MAX_VALUE, "김수신", "딸", "auth-code")

        assertEquals(Long.MAX_VALUE, listOf(receiver).toAfternoteEditorReceivers().single().id)
    }

    @Test
    fun `Long receiver id 로 중복을 막고 삭제한다`() {
        val receiverId = Long.MAX_VALUE

        val added =
            EditorFormState()
                .withReceiverAddedIfAbsent(receiverId, "김수신", "딸")
                .withReceiverAddedIfAbsent(receiverId, "중복 이름", "중복 관계")

        assertEquals(listOf(receiverId), added.afternoteEditReceivers.map { it.id })
        assertTrue(added.withReceiverDeleted(receiverId).afternoteEditReceivers.isEmpty())
    }

    @Test
    fun `receiver list expanded 상태는 Long id 를 키로 사용한다`() {
        val receiverId = Long.MAX_VALUE
        val state = AfternoteEditorReceiverListState()
        val receiver = AfternoteEditorReceiver(receiverId, "김수신", "딸")

        state.initializeExpandedStates(listOf(receiver), receiverId)

        assertTrue(state.expandedStates.getValue(receiverId))
        state.toggleItemExpanded(receiverId)
        assertFalse(state.expandedStates.getValue(receiverId))
    }
}
