package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * 에디터 "남기실 말씀" → 서버 페이로드 변환 회귀 가드 (이슈 #509).
 *
 * 서버가 블록 본문을 필수로 검증하므로, 에디터가 항상 띄워 두는 빈 블록이 그대로 실리면 등록이 400 으로
 * 거절된다. 구분자로 한 문자열에 인코딩하던 시절의 `EditorMessagesCodec` 을 대신하는 경계다.
 */
class EditorLeaveMessageBlocksTest {
    @Test
    fun `저장 빌더는 별도로 받은 현재 말씀을 payload에 싣는다`() {
        val blocks = listOf(EditorMessageTextBlock(title = "가족에게", body = "잘 부탁해"))

        val payload =
            SaveAfternotePayloadBuilder.build(
                form = EditorFormState(typeForm = AfternoteTypeForm.Social(selectedService = "인스타그램")),
                messageBlocks = blocks,
                accountId = "account",
                password = "password",
            )

        assertEquals(blocks, payload.messageBlocks)
    }

    @Test
    fun `아무것도 안 쓴 빈 칸은 전송에서 제외된다`() {
        val blocks =
            createSocial(
                EditorMessageTextBlock(title = "", body = ""),
                EditorMessageTextBlock(title = "   ", body = "   "),
                EditorMessageTextBlock(title = "가족에게", body = "잘 부탁해"),
            )

        assertEquals(listOf(LeaveMessageBlock(title = "가족에게", body = "잘 부탁해")), blocks)
    }

    /** 제목만 쓴 블록을 조용히 버리면 입력이 사라진다. 서버도 본문 없는 블록을 400 으로 막는다. */
    @Test
    fun `제목만 쓰고 본문을 비우면 저장이 막힌다`() {
        val error =
            validateSocial(
                EditorMessageTextBlock(title = "가족에게", body = "   "),
            )

        assertEquals(AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED, error)
    }

    @Test
    fun `제목도 본문도 안 쓴 빈 칸은 저장을 막지 않는다`() {
        val error = validateSocial(EditorMessageTextBlock(title = "", body = ""))

        assertNotEquals(AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED, error)
    }

    /** 필드를 뺄지는 data 계층 `toDto` 가 정하므로, 여기서는 빈 목록까지만 만든다. */
    @Test
    fun `남는 블록이 없으면 빈 목록이다`() {
        assertEquals(emptyList<LeaveMessageBlock>(), createSocial(EditorMessageTextBlock(title = "", body = "")))
        assertEquals(emptyList<LeaveMessageBlock>(), createSocial())
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
                type = AfternoteType.SOCIAL_NETWORK,
                payload =
                    payloadOf(
                        EditorMessageTextBlock(
                            title = "",
                            body = "고쳐 쓴 말씀",
                            isRegistered = true,
                        ),
                    ),
                selectedReceiverIds = emptyList(),
                playlistSongs = emptyList(),
                memorialMedia = MemorialMediaUrls(),
                // 여기서 보는 것은 블록을 *어떻게 빚는가* 다. 말씀이 없던 노트를 기준으로 삼아
                // 「달라졌다」가 확실하게 성립하게 두고, 빚어진 모양만 본다 — 안 건드린 필드를 빼는
                // 축은 AfternoteEditorPartialUpdateTest 가 따로 고정한다 (#1617).
                baseline = emptyBaseline(AfternoteType.SOCIAL_NETWORK),
            )

        assertEquals(
            listOf(LeaveMessageBlock(title = null, body = "고쳐 쓴 말씀")),
            payload.leaveMessageBlocks,
        )
    }

    /** 생성 payload 에 `leaveMessageBlocks` 가 없어 입력이 조용히 버려지던 회귀 가드 (이슈 #678). */
    @Test
    fun `메모리얼 생성도 남기실 말씀을 싣는다`() {
        val input =
            AfternoteEditorFormMapper.buildCreateInput(
                type = AfternoteType.MEMORIAL,
                payload = payloadOf(EditorMessageTextBlock(title = "가족에게", body = "잘 지내")),
                selectedReceiverIds = emptyList(),
                playlistSongs = emptyList(),
                memorialVideoUrl = null,
                memorialThumbnailUrl = null,
                memorialPhotoUrl = null,
            )

        assertEquals(
            listOf(LeaveMessageBlock(title = "가족에게", body = "잘 지내")),
            (input as CreateAfternoteInput.Memorial).payload.leaveMessageBlocks,
        )
    }

    @Test
    fun `메모리얼 수정도 기존 남기실 말씀을 다시 싣는다`() {
        val payload =
            AfternoteEditorFormMapper.buildUpdatePayload(
                type = AfternoteType.MEMORIAL,
                payload = payloadOf(EditorMessageTextBlock(title = "가족에게", body = "잘 지내")),
                selectedReceiverIds = emptyList(),
                playlistSongs = emptyList(),
                memorialMedia = MemorialMediaUrls(),
                // 여기서 보는 것은 블록을 *어떻게 빚는가* 다. 말씀이 없던 노트를 기준으로 삼아
                // 「달라졌다」가 확실하게 성립하게 두고, 빚어진 모양만 본다 (#1617).
                baseline = emptyBaseline(AfternoteType.MEMORIAL),
            )

        assertEquals(
            listOf(LeaveMessageBlock(title = "가족에게", body = "잘 지내")),
            payload.leaveMessageBlocks,
        )
    }

    private fun createSocial(vararg blocks: EditorMessageTextBlock): List<LeaveMessageBlock>? {
        val input =
            AfternoteEditorFormMapper.buildCreateInput(
                type = AfternoteType.SOCIAL_NETWORK,
                payload = payloadOf(*blocks),
                selectedReceiverIds = emptyList(),
                playlistSongs = emptyList(),
                memorialVideoUrl = null,
                memorialThumbnailUrl = null,
                memorialPhotoUrl = null,
            )
        return (input as CreateAfternoteInput.Social).payload.leaveMessageBlocks
    }

    private fun validateSocial(vararg blocks: EditorMessageTextBlock): AfternoteValidationError? =
        AfternoteEditorValidator.validate(
            form = EditorFormState(typeForm = AfternoteTypeForm.Social()),
            payload =
                payloadOf(*blocks).copy(
                    accountId = "account",
                    password = "password",
                    processingMethods = listOf("계정 삭제"),
                ),
        )

    private fun payloadOf(vararg blocks: EditorMessageTextBlock) =
        RegisterAfternotePayload(
            serviceName = "인스타그램",
            date = "2026.08.07",
            messageBlocks = blocks.toList(),
        )

    /** 말씀도 내용도 없던 노트 — 폼에 무엇이 들어 있든 「달라졌다」가 성립하는 기준. */
    private fun emptyBaseline(type: AfternoteType) =
        AfternoteEditorFormMapper.buildUpdateBaseline(
            Detail(
                id = 1L,
                serviceName = "",
                timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
                receivers = emptyList(),
                leaveMessageBlocks = emptyList(),
                content =
                    when (type) {
                        AfternoteType.MEMORIAL -> {
                            DetailContent.Memorial(
                                memorial = MemorialDetail(emptyList(), MemorialMedia(null, null, null)),
                            )
                        }

                        else -> {
                            DetailContent.SocialNetwork(
                                credentials = DetailCredentials(id = "", password = ""),
                                processingMethods = emptyList(),
                            )
                        }
                    },
            ),
        )
}
