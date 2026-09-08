package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AfternoteEditorFormPrefillTest {
    @Test
    fun `소셜 프리필은 계정은 소셜 타입에 남기실 말씀은 공통 필드에 담는다`() {
        val prefill =
            AfternoteEditorFormMapper.buildEditorFormPrefill(
                detail(
                    serviceName = "인스타그램",
                    leaveMessageBlocks = listOf(LeaveMessageBlock(title = null, body = "부탁해")),
                    content =
                        DetailContent.SocialNetwork(
                            credentials = DetailCredentials(id = "account", password = "password"),
                            processingMethods = listOf("계정 삭제"),
                        ),
                ),
            )
        val content = prefill.content as EditorContentPrefill.SocialNetwork

        assertEquals("인스타그램", content.serviceName)
        assertEquals("account", content.credentials.id)
        assertEquals("password", content.credentials.password)
        assertEquals("부탁해", prefill.leaveMessageBlocks.single().body)
        assertTrue(prefill.leaveMessageBlocks.single().isRegistered)
        assertEquals("계정 삭제", content.processingMethods.single().text)
    }

    @Test
    fun `갤러리 프리필은 계정 없이 남기실 말씀과 처리 방법만 담는다`() {
        val prefill =
            AfternoteEditorFormMapper.buildEditorFormPrefill(
                detail(
                    serviceName = "구글 포토",
                    leaveMessageBlocks = listOf(LeaveMessageBlock(title = "사진", body = "보관해 줘")),
                    content =
                        DetailContent.Gallery(
                            processingMethods = listOf("파일 전달"),
                        ),
                ),
            )
        val content = prefill.content as EditorContentPrefill.Gallery

        assertEquals("구글 포토", content.serviceName)
        assertEquals("사진", prefill.leaveMessageBlocks.single().title)
        assertEquals("파일 전달", content.processingMethods.single().text)
    }

    @Test
    fun `메모리얼 프리필은 계정 없이 미디어 곡과 공통 남기실 말씀을 담는다`() {
        val prefill =
            AfternoteEditorFormMapper.buildEditorFormPrefill(
                detail(
                    serviceName = "추억 노트",
                    leaveMessageBlocks = listOf(LeaveMessageBlock(title = "가족에게", body = "잘 지내")),
                    content =
                        DetailContent.Memorial(
                            memorial =
                                MemorialDetail(
                                    songs =
                                        listOf(
                                            DetailSong(
                                                title = "노래",
                                                artist = "가수",
                                                coverUrl = "cover",
                                            ),
                                        ),
                                    media =
                                        MemorialMedia(
                                            photoUrl = "photo",
                                            videoUrl = "video",
                                            thumbnailUrl = "thumbnail",
                                        ),
                                ),
                        ),
                ),
            )
        val content = prefill.content as EditorContentPrefill.Memorial

        assertEquals("photo", content.photoUrl)
        assertEquals("video", content.videoUrl)
        assertEquals("thumbnail", content.thumbnailUrl)
        assertEquals("detail:0", content.playlistSongs.single().selectionKey)
        assertEquals("잘 지내", prefill.leaveMessageBlocks.single().body)
    }

    @Test
    fun `메모리얼 저장은 UI 선택 키를 도메인 입력에 포함하지 않는다`() {
        val input =
            AfternoteEditorFormMapper.buildCreateInput(
                type = AfternoteType.MEMORIAL,
                payload = RegisterAfternotePayload(serviceName = "추억 노트", date = "2026-08-21"),
                selectedReceiverIds = emptyList(),
                playlistSongs =
                    listOf(
                        Song(
                            selectionKey = "detail:0",
                            title = "기존 노래",
                            artist = "가수",
                        ),
                        Song(
                            selectionKey = "search:가수|검색한 노래|0",
                            title = "검색한 노래",
                            artist = "가수",
                        ),
                    ),
                memorialVideoUrl = null,
                memorialThumbnailUrl = null,
                memorialPhotoUrl = null,
            ) as CreateAfternoteInput.Memorial

        assertEquals(
            listOf(
                MemorialSongPayload(
                    title = "기존 노래",
                    artist = "가수",
                    coverUrl = null,
                ),
                MemorialSongPayload(
                    title = "검색한 노래",
                    artist = "가수",
                    coverUrl = null,
                ),
            ),
            input.payload.memorial.songs,
        )
    }

    private fun detail(
        serviceName: String,
        leaveMessageBlocks: List<LeaveMessageBlock>,
        content: DetailContent,
    ) = Detail(
        id = 1L,
        serviceName = serviceName,
        timestamps = DetailTimestamps(updatedAt = "2026-08-21"),
        receivers = emptyList(),
        leaveMessageBlocks = leaveMessageBlocks,
        content = content,
    )
}
