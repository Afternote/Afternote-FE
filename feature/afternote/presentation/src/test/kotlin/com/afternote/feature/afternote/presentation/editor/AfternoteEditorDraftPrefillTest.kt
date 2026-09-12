package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.DraftDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 임시저장 상세 → 에디터 프리필, 그리고 임시저장 여부가 저장 입력에 실리는 경계 (#808).
 *
 * 임시저장은 「미완성을 그대로 보존한 것」이라 종류별 값이 통째로 비어 올 수 있다 — 그 «없음» 이
 * 빈 입력칸이 되어야 이어쓰기가 성립한다. 발행 프리필의 계약은 [AfternoteEditorReceiverPrefillTest] 가 지킨다.
 */
class AfternoteEditorDraftPrefillTest {
    @Test
    fun `곡을 안 담은 임시저장은 빈 추억 노트 폼으로 열린다`() {
        val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(draft(AfternoteType.MEMORIAL))

        val content = prefill.content as EditorContentPrefill.Memorial
        assertTrue(content.playlistSongs.isEmpty())
        assertEquals(null, content.photoUrl)
        assertEquals(null, content.videoUrl)
    }

    @Test
    fun `계정 정보를 아직 안 쓴 임시저장은 빈 입력칸으로 열린다`() {
        val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(draft(AfternoteType.SOCIAL_NETWORK))

        val content = prefill.content as EditorContentPrefill.SocialNetwork
        assertEquals("", content.credentials.id)
        assertEquals("", content.credentials.password)
    }

    @Test
    fun `임시저장 상세의 수신자는 발행 상세와 같게 옮겨진다`() {
        val prefill =
            AfternoteEditorFormMapper.buildEditorFormPrefill(
                draft(AfternoteType.GALLERY_AND_FILES, DetailReceiver(receiverId = 7L, name = "김수신", relation = "딸")),
            )

        assertEquals(listOf(7L), prefill.receivers.map { it.id })
        assertEquals("김수신", prefill.receivers.single().name)
    }

    @Test
    fun `임시저장 저장은 생성 입력에 isDraft 를 싣고 정식 등록은 안 싣는다`() {
        val input =
            CreateAfternoteInput.Gallery(
                com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload(
                    title = "t",
                    processingMethods = emptyList(),
                ),
            )

        val draftInput = AfternoteEditorFormMapper.withDraft(input, isDraft = true) as CreateAfternoteInput.Gallery
        val publishInput = AfternoteEditorFormMapper.withDraft(input, isDraft = false) as CreateAfternoteInput.Gallery

        assertTrue(draftInput.payload.isDraft)
        assertEquals(false, publishInput.payload.isDraft)
    }

    private fun draft(
        type: AfternoteType,
        vararg receivers: DetailReceiver,
    ) = DraftDetail(
        id = 1L,
        type = type,
        serviceName = "제목만 쓴 노트",
        timestamps = DetailTimestamps(updatedAt = "2026-09-03"),
        receivers = receivers.toList(),
        leaveMessageBlocks = emptyList(),
        credentials = null,
        processingMethods = emptyList(),
        songs = emptyList(),
        media = MemorialMedia(photoUrl = null, videoUrl = null, thumbnailUrl = null),
    )
}
