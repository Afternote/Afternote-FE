package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 에디터 "남기실 말씀" → 서버 페이로드 변환 회귀 가드 (이슈 #509).
 *
 * 서버가 블록 본문을 필수로 검증하므로, 에디터가 항상 띄워 두는 빈 블록이 그대로 실리면 등록이 400 으로
 * 거절된다. 구분자로 한 문자열에 인코딩하던 시절의 `EditorMessagesCodec` 을 대신하는 경계다.
 */
class EditorLeaveMessageBlocksTest {
    @Test
    fun `본문이 빈 블록은 전송에서 제외된다`() {
        val blocks =
            createSocial(
                EditorMessageTextBlock(title = "제목만", body = ""),
                EditorMessageTextBlock(title = "공백만", body = "   "),
                EditorMessageTextBlock(title = "가족에게", body = "잘 부탁해"),
            )

        assertEquals(listOf(LeaveMessageBlock(title = "가족에게", body = "잘 부탁해")), blocks)
    }

    @Test
    fun `남는 블록이 없으면 null 이라 필드가 실리지 않는다`() {
        assertNull(createSocial(EditorMessageTextBlock(title = "", body = "")))
        assertNull(createSocial())
    }

    @Test
    fun `제목이 비어 있으면 null 로 보내고 앞뒤 공백은 지운다`() {
        val blocks = createSocial(EditorMessageTextBlock(title = "  ", body = "  본문  "))

        assertEquals(listOf(LeaveMessageBlock(title = null, body = "본문")), blocks)
    }

    @Test
    fun `수정 페이로드도 같은 규칙으로 블록을 싣는다`() {
        val payload =
            AfternoteEditorFormMapper.buildUpdatePayload(
                category = EditorCategory.SOCIAL,
                payload = payloadOf(EditorMessageTextBlock(title = "", body = "고쳐 쓴 말씀")),
                selectedReceiverIds = emptyList(),
                playlistSongs = emptyList(),
                memorialMedia = MemorialMediaUrls(),
            )

        assertEquals(
            listOf(LeaveMessageBlock(title = null, body = "고쳐 쓴 말씀")),
            payload.leaveMessageBlocks,
        )
    }

    private fun createSocial(vararg blocks: EditorMessageTextBlock): List<LeaveMessageBlock>? {
        val input =
            AfternoteEditorFormMapper.buildCreateInput(
                category = EditorCategory.SOCIAL,
                payload = payloadOf(*blocks),
                selectedReceiverIds = emptyList(),
                playlistSongs = emptyList(),
                memorialVideoUrl = null,
                memorialThumbnailUrl = null,
                memorialPhotoUrl = null,
            )
        return (input as CreateAfternoteInput.Social).payload.leaveMessageBlocks
    }

    private fun payloadOf(vararg blocks: EditorMessageTextBlock) =
        RegisterAfternotePayload(
            serviceName = "인스타그램",
            date = "2026.08.07",
            messageBlocks = blocks.toList(),
        )
}
