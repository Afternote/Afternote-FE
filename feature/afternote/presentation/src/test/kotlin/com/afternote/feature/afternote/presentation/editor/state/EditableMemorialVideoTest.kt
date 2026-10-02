package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.repository.author.MediaInput
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** 추모 영상 칸의 편집 상태 계약 — 저장 때 올려야 하는지, 이미 올라가 있는지, 없는지. */
class EditableMemorialVideoTest {
    private val serverVideo =
        MemorialVideoAttachment(
            url = "https://cdn.test/farewell.mp4",
            thumbnailUrl = "https://cdn.test/farewell-thumb.jpg",
        )
    private val pickedVideo =
        MemorialVideoAttachment(
            url = "content://videos/replacement",
            thumbnailUrl = "https://cdn.test/replacement-thumb.jpg",
        )

    private fun picked(attachment: MemorialVideoAttachment): EditableMemorialVideo =
        EditableMemorialVideo
            .empty()
            .withSelection(attachment.url)
            .let { video -> attachment.thumbnailUrl?.let(video::withSelectionThumbnail) ?: video }

    @Test
    fun `세 상태는 표시 영상과 저장 입력을 스스로 결정한다`() {
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
                    name = "영상 없음",
                    subject = EditableMemorialVideo.empty(),
                    displayed = null,
                    canRemove = false,
                    mediaInput = MediaInput.None,
                ),
                Case(
                    name = "이미 올라간 영상",
                    subject = EditableMemorialVideo.fromServer(serverVideo),
                    displayed = serverVideo,
                    canRemove = true,
                    mediaInput = MediaInput.Remote(serverVideo.url),
                ),
                Case(
                    name = "올릴 영상",
                    subject = picked(pickedVideo),
                    displayed = pickedVideo,
                    canRemove = true,
                    mediaInput = MediaInput.Local(pickedVideo.url),
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
    fun `서버 영상 위에 새 영상을 고르면 서버 영상은 잊고 올릴 영상만 남는다`() {
        val replaced = EditableMemorialVideo.fromServer(serverVideo).withSelection(pickedVideo.url)

        // #2114: 넷째 상태(Replaced)를 없앴다. 서버 원본을 뒤에 들고 있어도 읽는 경로가 없었다.
        assertEquals(picked(MemorialVideoAttachment(url = pickedVideo.url)), replaced)
        assertEquals(MediaInput.Local(pickedVideo.url), replaced.toMediaInput())
    }

    @Test
    fun `저장 출처는 URL 모양이 아니라 객체가 기억한 편집 상태로 결정한다`() {
        val remoteShapedSelection = "https://picker.test/new-video.mp4"
        val localShapedServerVideo = "content://migrated/server-video"

        assertEquals(
            MediaInput.Local(remoteShapedSelection),
            EditableMemorialVideo
                .empty()
                .withSelection(remoteShapedSelection)
                .toMediaInput(),
        )
        assertEquals(
            MediaInput.Remote(localShapedServerVideo),
            EditableMemorialVideo
                .fromServer(MemorialVideoAttachment(url = localShapedServerVideo))
                .toMediaInput(),
        )
    }

    @Test
    fun `새 영상을 고르면 이전 선택의 썸네일은 물려주지 않는다`() {
        val selected = picked(pickedVideo).withSelection("content://videos/another")

        assertEquals(MemorialVideoAttachment(url = "content://videos/another"), selected.displayed)
        assertEquals(MediaInput.Local("content://videos/another"), selected.toMediaInput())
    }

    @Test
    fun `썸네일은 올릴 영상에만 붙고 올릴 영상이 없으면 늦은 결과를 버린다`() {
        val withThumbnail =
            EditableMemorialVideo
                .fromServer(serverVideo)
                .withSelection(pickedVideo.url)
                .withSelectionThumbnail("https://cdn.test/new-thumb.jpg")

        assertEquals("https://cdn.test/new-thumb.jpg", withThumbnail.displayed?.thumbnailUrl)

        val afterLateThumbnail =
            EditableMemorialVideo
                .fromServer(serverVideo)
                .withSelectionThumbnail("https://cdn.test/late-thumb.jpg")

        assertEquals(serverVideo, afterLateThumbnail.displayed)
    }

    @Test
    fun `썸네일을 떼도 출처는 남는다`() {
        assertEquals(
            EditableMemorialVideo.fromServer(MemorialVideoAttachment(url = serverVideo.url)),
            EditableMemorialVideo.fromServer(serverVideo).withoutThumbnail(),
        )
        // 서버 영상도 지문에 남아야 수정 진입 기준선과 비교해 서버 영상 삭제가 변경으로 잡힌다(#1597).
        assertEquals(
            EditableMemorialVideo.empty().withSelection(pickedVideo.url),
            picked(pickedVideo).withoutThumbnail(),
        )
    }

    @Test
    fun `세 상태 모두 JSON 왕복 뒤에 같은 상태로 돌아온다`() {
        // SavedStateHandle 이 실제로 쓰는 설정 그대로 — AfternoteEditorViewModel.formSnapshotJson.
        val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        val states =
            mapOf(
                "영상 없음" to EditableMemorialVideo.empty(),
                "이미 올라간 영상" to EditableMemorialVideo.fromServer(serverVideo),
                "올릴 영상" to picked(pickedVideo),
            )

        states.forEach { (name, state) ->
            val roundTripped =
                json.decodeFromString(
                    EditableMemorialVideo.serializer(),
                    json.encodeToString(EditableMemorialVideo.serializer(), state),
                )

            assertEquals(name, state, roundTripped)
            assertEquals(name, state.displayed, roundTripped.displayed)
            assertEquals(name, state.canRemove, roundTripped.canRemove)
            assertEquals(name, state.toMediaInput(), roundTripped.toMediaInput())
        }
    }

    @Test
    fun `판별자 값은 클래스 이름이 아니라 고정 문자열이다`() {
        val json = Json { encodeDefaults = true }

        assertEquals("""{"type":"no_video"}""", json.encodeToString(EditableMemorialVideo.serializer(), EditableMemorialVideo.empty()))
        assertEquals(
            """{"type":"uploaded","video":{"url":"${serverVideo.url}","thumbnailUrl":"${serverVideo.thumbnailUrl}"}}""",
            json.encodeToString(EditableMemorialVideo.serializer(), EditableMemorialVideo.fromServer(serverVideo)),
        )
        assertEquals(
            """{"type":"pending_upload","video":{"url":"${pickedVideo.url}","thumbnailUrl":"${pickedVideo.thumbnailUrl}"}}""",
            json.encodeToString(EditableMemorialVideo.serializer(), picked(pickedVideo)),
        )
    }
}
