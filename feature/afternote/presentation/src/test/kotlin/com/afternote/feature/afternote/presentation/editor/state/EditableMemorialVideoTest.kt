package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import org.junit.Assert.assertEquals
import org.junit.Test

/** 서버 원본과 이번 편집의 교체분을 함께 보존하는 영상 편집 값 객체의 상태 계약. */
class EditableMemorialVideoTest {
    private val persisted =
        MemorialVideoAttachment(
            url = "https://cdn.test/farewell.mp4",
            thumbnailUrl = "https://cdn.test/farewell-thumb.jpg",
        )
    private val selection =
        MemorialVideoAttachment(
            url = "content://videos/replacement",
            thumbnailUrl = "https://cdn.test/replacement-thumb.jpg",
        )

    private fun selected(attachment: MemorialVideoAttachment): EditableMemorialVideo =
        EditableMemorialVideo
            .empty()
            .withSelection(attachment.url)
            .let { video -> attachment.thumbnailUrl?.let(video::withSelectionThumbnail) ?: video }

    private fun persistedAndSelection(): EditableMemorialVideo =
        EditableMemorialVideo
            .fromPersisted(persisted)
            .withSelection(selection.url)
            .let { video -> selection.thumbnailUrl?.let(video::withSelectionThumbnail) ?: video }

    @Test
    fun `네 가지 상태는 표시 영상과 저장 입력을 스스로 결정한다`() {
        data class Case(
            val name: String,
            val subject: EditableMemorialVideo,
            val displayed: MemorialVideoAttachment?,
            val canRemove: Boolean,
            val mediaInput: MediaInput,
        )

        val cases =
            listOf(
                Case(
                    name = "빈 상태",
                    subject = EditableMemorialVideo.empty(),
                    displayed = null,
                    canRemove = false,
                    mediaInput = MediaInput.None,
                ),
                Case(
                    name = "서버 원본만",
                    subject = EditableMemorialVideo.fromPersisted(persisted),
                    displayed = persisted,
                    canRemove = true,
                    mediaInput = MediaInput.Remote(persisted.url),
                ),
                Case(
                    name = "새 선택만",
                    subject = selected(selection),
                    displayed = selection,
                    canRemove = true,
                    mediaInput = MediaInput.Local(selection.url),
                ),
                Case(
                    name = "서버 원본과 새 선택 모두",
                    subject = persistedAndSelection(),
                    displayed = selection,
                    canRemove = true,
                    mediaInput = MediaInput.Local(selection.url),
                ),
            )

        cases.forEach { case ->
            assertEquals("${case.name}: 표시 영상", case.displayed, case.subject.displayed)
            assertEquals(
                "${case.name}: 삭제 항목 노출 여부",
                case.canRemove,
                case.subject.canRemove,
            )
            assertEquals("${case.name}: 저장 입력", case.mediaInput, case.subject.toMediaInput())
        }
    }

    @Test
    fun `저장 출처는 URL 모양이 아니라 객체가 기억한 편집 상태로 결정한다`() {
        val remoteShapedSelection = "https://picker.test/new-video.mp4"
        val localShapedPersisted = "content://migrated/server-video"

        assertEquals(
            MediaInput.Local(remoteShapedSelection),
            EditableMemorialVideo
                .empty()
                .withSelection(remoteShapedSelection)
                .toMediaInput(),
        )
        assertEquals(
            MediaInput.Remote(localShapedPersisted),
            EditableMemorialVideo
                .fromPersisted(MemorialVideoAttachment(url = localShapedPersisted))
                .toMediaInput(),
        )
    }

    @Test
    fun `새 영상을 고르면 이전 교체분 썸네일은 물려주지 않는다`() {
        val selected =
            persistedAndSelection().withSelection("content://videos/another")

        assertEquals(MemorialVideoAttachment(url = "content://videos/another"), selected.displayed)
        assertEquals(MediaInput.Local("content://videos/another"), selected.toMediaInput())
    }

    @Test
    fun `썸네일은 교체분에만 붙고 교체분이 없으면 늦은 결과를 버린다`() {
        val withThumbnail =
            EditableMemorialVideo
                .fromPersisted(persisted)
                .withSelection(selection.url)
                .withSelectionThumbnail("https://cdn.test/new-thumb.jpg")

        assertEquals("https://cdn.test/new-thumb.jpg", withThumbnail.displayed?.thumbnailUrl)

        val afterLateThumbnail =
            EditableMemorialVideo
                .fromPersisted(persisted)
                .withSelectionThumbnail("https://cdn.test/late-thumb.jpg")

        assertEquals(persisted, afterLateThumbnail.displayed)
    }

    @Test
    fun `썸네일을 떼도 서버 원본과 교체분은 두 층 다 남는다`() {
        val stripped =
            persistedAndSelection().withoutThumbnail()

        assertEquals(MemorialVideoAttachment(url = selection.url), stripped.displayed)
        // 서버 원본도 지문에 남아야 수정 진입 기준선과 비교해 서버 원본 삭제가 변경으로 잡힌다(#1597).
        assertEquals(
            EditableMemorialVideo.fromPersisted(MemorialVideoAttachment(url = persisted.url)).withSelection(selection.url),
            stripped,
        )
    }
}
