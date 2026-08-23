package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 수정 진입 시 기존 수신자 프리필 계약 회귀 가드 (#566).
 *
 * 프리필이 비면 저장이 `수신자를 한 명 이상 선택해 주세요.` 로 막혀 **어떤 수정도 저장할 수 없다**.
 * 폼의 수신자 id 는 저장 시 `AfternoteNavGraphEditor` 가 `mapNotNull { it.id.toLongOrNull() }` 로
 * 되돌려 쓰므로, id 왕복이 깨지지 않는 것까지 함께 고정한다.
 */
class AfternoteEditorReceiverPrefillTest {
    @Test
    fun `상세의 수신자가 폼 프리필로 옮겨진다`() {
        val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(detail(receiver(7L, "김수신", "딸")))

        assertEquals(1, prefill.receivers.size)
        assertEquals("7", prefill.receivers.single().id)
        assertEquals("김수신", prefill.receivers.single().name)
        assertEquals("딸", prefill.receivers.single().label)
    }

    /** 저장 payload 가 폼 id 를 Long 으로 되돌리므로 이 왕복이 깨지면 수신자가 조용히 탈락한다. */
    @Test
    fun `프리필된 id 는 저장 payload 의 Long 으로 되돌아간다`() {
        val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(detail(receiver(7L, "김수신", "딸")))

        assertEquals(listOf(7L), prefill.receivers.mapNotNull { it.id.toLongOrNull() })
    }

    @Test
    fun `수신자가 없는 상세는 빈 목록이 된다`() {
        val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(detail())

        assertEquals(emptyList<String>(), prefill.receivers.map { it.id })
    }

    private fun receiver(
        id: Long,
        name: String,
        relation: String,
    ) = DetailReceiver(receiverId = id, name = name, relation = relation)

    private fun detail(vararg receivers: DetailReceiver) =
        Detail(
            id = 1L,
            category = "GALLERY_AND_FILES",
            title = "구글 포토",
            timestamps = DetailTimestamps(updatedAt = "2026-08-02"),
            type = AfternoteType.entries.first(),
            credentials = null,
            receivers = receivers.toList(),
            processingMethods = emptyList(),
            leaveMessageBlocks = emptyList(),
            memorial = null,
        )
}
