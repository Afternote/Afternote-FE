package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.FieldPatch
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 수정 요청이 **사용자가 만진 슬롯만** 싣는지 고정한다 (#1617).
 *
 * 종전에는 폼이 든 전체 스냅샷이 매번 통째로 나갔다. 에디터를 연 뒤 서버가 바뀌면, 사용자가 건드린
 * 적도 없는 필드가 낡은 로컬 값으로 덮였다.
 *
 * 고정하는 축은 셋이다.
 * 1. 안 건드린 슬롯은 페이로드에서 빠진다 → 요청에서 키째 빠진다 → 서버가 「유지」로 읽는다.
 * 2. **비운 것은 안 건드린 것과 다르다** — 빈 목록·명시적 삭제는 그대로 실린다 (#1596·#1599).
 * 3. **판정 단위는 서버가 반영하는 단위와 같다** — 계정 정보는 id·비밀번호를 따로, 플레이리스트는
 *    사진·영상·곡을 따로 잰다. 객체째 재면 안 건드린 형제 슬롯이 낡은 값으로 딸려 나간다.
 */
class AfternoteEditorPartialUpdateTest {
    private val socialDetail =
        Detail(
            id = 7L,
            serviceName = "인스타그램",
            timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
            receivers =
                listOf(
                    DetailReceiver(receiverId = 11L, name = "엄마", relation = "가족"),
                    DetailReceiver(receiverId = 22L, name = "동생", relation = "가족"),
                ),
            leaveMessageBlocks = listOf(LeaveMessageBlock(title = "가족에게", body = "고마웠어")),
            content =
                DetailContent.SocialNetwork(
                    credentials = DetailCredentials(id = "account", password = "pw"),
                    processingMethods = listOf("계정 삭제", "사진 백업"),
                ),
        )

    private val memorialDetail =
        Detail(
            id = 9L,
            serviceName = "추억 노트",
            timestamps = DetailTimestamps(updatedAt = "2026-08-30"),
            receivers = emptyList(),
            leaveMessageBlocks = emptyList(),
            content =
                DetailContent.Memorial(
                    memorial =
                        MemorialDetail(
                            songs = listOf(DetailSong(title = "곡", artist = "가수", coverUrl = null)),
                            media =
                                MemorialMedia(
                                    photoUrl = "https://cdn.test/afternotes/photo.jpg",
                                    videoUrl = "https://cdn.test/afternotes/video.mp4",
                                    thumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                                ),
                        ),
                ),
        )

    /** 상세를 그대로 되돌린 폼 — 「아무것도 안 건드린 사용자」다. */
    private fun untouchedSocialPayload(
        serviceName: String = "인스타그램",
        accountId: String = "account",
        password: String = "pw",
        processingMethods: List<String> = listOf("계정 삭제", "사진 백업"),
        messageBlocks: List<EditorMessageTextBlock> =
            listOf(EditorMessageTextBlock(title = "가족에게", body = "고마웠어", isRegistered = true)),
    ) = RegisterAfternotePayload(
        serviceName = serviceName,
        date = "2026-08-30",
        accountId = accountId,
        password = password,
        messageBlocks = messageBlocks,
        processingMethods = processingMethods,
    )

    private fun buildSocialUpdate(
        payload: RegisterAfternotePayload = untouchedSocialPayload(),
        selectedReceiverIds: List<Long> = listOf(11L, 22L),
        detail: Detail = socialDetail,
    ) = AfternoteEditorFormMapper.buildUpdatePayload(
        type = AfternoteType.SOCIAL_NETWORK,
        payload = payload,
        selectedReceiverIds = selectedReceiverIds,
        playlistSongs = emptyList(),
        memorialMedia = MemorialMediaUrls(),
        baseline = AfternoteEditorFormMapper.buildUpdateBaseline(detail),
    )

    @Test
    fun `제목만 고치면 제목 말고는 아무 필드도 실리지 않는다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(serviceName = "새 제목"))

        assertEquals("새 제목", updated.title)
        assertNull("처리 방법을 만진 적이 없다", updated.processingMethods)
        assertNull("남기실 말씀을 만진 적이 없다", updated.leaveMessageBlocks)
        assertNull("계정 정보를 만진 적이 없다", updated.credentials)
        assertNull("수신자를 만진 적이 없다", updated.receivers)
        assertNull("추억 노트가 아니므로 애초에 없다", updated.memorial)
    }

    @Test
    fun `아무것도 안 고치면 어느 필드도 실리지 않는다`() {
        val updated = buildSocialUpdate()

        assertNull(updated.title)
        assertNull(updated.processingMethods)
        assertNull(updated.leaveMessageBlocks)
        assertNull(updated.credentials)
        assertNull(updated.receivers)
    }

    /** 순서가 뜻을 갖지 않는 수신자는 정렬이 달라졌다고 「고쳤다」로 읽지 않는다. */
    @Test
    fun `수신자 순서만 바뀐 것은 변경이 아니다`() {
        val updated = buildSocialUpdate(selectedReceiverIds = listOf(22L, 11L))

        assertNull(updated.receivers)
    }

    @Test
    fun `수신자를 실제로 지우면 남은 목록이 실린다`() {
        val updated = buildSocialUpdate(selectedReceiverIds = listOf(11L))

        assertEquals(listOf(ReceiverRefPayload(receiverId = 11L)), updated.receivers)
    }

    @Test
    fun `수신자를 전부 빼면 빈 목록이 실려 전부 삭제로 나간다`() {
        val updated = buildSocialUpdate(selectedReceiverIds = emptyList())

        assertEquals(emptyList<ReceiverRefPayload>(), updated.receivers)
    }

    @Test
    fun `처리 방법을 전부 빼면 빈 목록이 실려 전부 삭제로 나간다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(processingMethods = emptyList()))

        assertEquals(emptyList<String>(), updated.processingMethods)
    }

    /**
     * 서버는 `actions` 원소를 검증 없이 저장하므로 `[""]` 가 실제로 남아 있을 수 있다. 비교 전에
     * 빈 문자열을 걷어내면 그 행을 지운 저장이 「양쪽 다 빈 목록」으로 상쇄돼 **삭제가 사라진다.**
     */
    @Test
    fun `서버에 빈 문자열 처리 방법만 있을 때 그 행을 지우면 삭제가 나간다`() {
        val detailWithBlank =
            socialDetail.copy(
                content =
                    DetailContent.SocialNetwork(
                        credentials = DetailCredentials(id = "account", password = "pw"),
                        processingMethods = listOf(""),
                    ),
            )
        val updated =
            buildSocialUpdate(
                payload = untouchedSocialPayload(processingMethods = emptyList()),
                detail = detailWithBlank,
            )

        assertEquals(emptyList<String>(), updated.processingMethods)
    }

    @Test
    fun `남기실 말씀을 전부 빼면 빈 목록이 실려 전부 삭제로 나간다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(messageBlocks = emptyList()))

        assertEquals(emptyList<LeaveMessageBlock>(), updated.leaveMessageBlocks)
    }

    /**
     * 서버가 id 와 비밀번호를 **따로** 갱신하므로(`CredentialsRelationSupport.update`), 안 고친 쪽은
     * 실으면 안 된다. 함께 실으면 그 사이 다른 기기가 바꾼 id 가 낡은 값으로 되돌아간다.
     */
    @Test
    fun `비밀번호만 고치면 아이디는 실리지 않는다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(password = "새 비밀번호"))

        assertEquals("새 비밀번호", updated.credentials?.password)
        assertNull("아이디는 만진 적이 없다", updated.credentials?.id)
        assertNull("제목은 그대로다", updated.title)
    }

    @Test
    fun `아이디만 고치면 비밀번호는 실리지 않는다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(accountId = "새 아이디"))

        assertEquals("새 아이디", updated.credentials?.id)
        assertNull("비밀번호는 만진 적이 없다", updated.credentials?.password)
    }

    /**
     * 에디터가 띄워 두는 빈 입력 칸은 도메인으로 넘어가기 전에 버려진다. 그 결과가 서버 원본과
     * 같다면 「고쳤다」가 아니다 — 여기서 흔들리면 저장할 때마다 남기실 말씀이 다시 실린다.
     */
    @Test
    fun `빈 입력 칸과 앞뒤 공백은 변경으로 세지 않는다`() {
        val updated =
            buildSocialUpdate(
                payload =
                    untouchedSocialPayload(
                        messageBlocks =
                            listOf(
                                EditorMessageTextBlock(title = " 가족에게 ", body = " 고마웠어 ", isRegistered = true),
                                EditorMessageTextBlock(title = "", body = "", isRegistered = false),
                            ),
                    ),
            )

        assertNull(updated.leaveMessageBlocks)
    }

    private fun buildMemorialUpdate(
        serviceName: String = "추억 노트",
        playlistSongs: List<Song> =
            listOf(Song(selectionKey = "detail:0", title = "곡", artist = "가수", albumCoverUrl = null)),
        memorialMedia: MemorialMediaUrls =
            MemorialMediaUrls(
                memorialVideoUrl = "https://cdn.test/afternotes/video.mp4",
                memorialThumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                memorialPhotoUrl = "https://cdn.test/afternotes/photo.jpg",
            ),
    ) = AfternoteEditorFormMapper.buildUpdatePayload(
        type = AfternoteType.MEMORIAL,
        payload = RegisterAfternotePayload(serviceName = serviceName, date = "2026-08-30"),
        selectedReceiverIds = emptyList(),
        playlistSongs = playlistSongs,
        memorialMedia = memorialMedia,
        baseline = AfternoteEditorFormMapper.buildUpdateBaseline(memorialDetail),
    )

    /**
     * 이슈가 든 재현 경로 그대로다 — 곡을 건드리지 않은 「제목만 수정」이 곡을 지우면 안 된다.
     * `playlist` 키가 아예 나가지 않아야 서버가 곡·사진·영상을 통째로 유지한다.
     */
    @Test
    fun `추억 노트도 제목만 고치면 플레이리스트를 말하지 않는다`() {
        val updated = buildMemorialUpdate(serviceName = "새 제목")

        assertEquals("새 제목", updated.title)
        assertNull("곡을 건드린 적이 없다", updated.memorial)
    }

    /**
     * **곡만 고친 저장이 사진·영상을 건드리면 안 된다.**
     *
     * 사진·영상 슬롯은 기본값이 없는 DTO 로 나가던 동안 `null` 이 곧 삭제였다. 곡을 바꿨다는 이유로
     * 셋을 함께 실으면, 그 사이 다른 기기가 올린 사진·영상이 명시적 null 로 지워진다.
     */
    @Test
    fun `곡만 고치면 사진과 영상 슬롯은 말하지 않는다`() {
        val updated =
            buildMemorialUpdate(
                playlistSongs =
                    listOf(Song(selectionKey = "detail:0", title = "새 곡", artist = "가수", albumCoverUrl = null)),
            )

        val memorial = requireNotNull(updated.memorial) { "곡을 고쳤으므로 플레이리스트가 실려야 한다" }
        assertEquals(
            listOf(MemorialSongPayload(title = "새 곡", artist = "가수", coverUrl = null)),
            memorial.songs,
        )
        assertEquals("사진을 건드린 적이 없다", FieldPatch.Unchanged, memorial.memorialPhotoUrl)
        assertEquals("영상을 건드린 적이 없다", FieldPatch.Unchanged, memorial.memorialVideo)
    }

    @Test
    fun `곡을 전부 빼면 빈 곡 목록만 실리고 미디어는 말하지 않는다`() {
        val updated = buildMemorialUpdate(playlistSongs = emptyList())

        val memorial = requireNotNull(updated.memorial) { "곡을 지웠으므로 플레이리스트가 실려야 한다" }
        assertTrue("곡을 지운 것은 명시적 삭제다", memorial.songs?.isEmpty() == true)
        assertEquals(FieldPatch.Unchanged, memorial.memorialPhotoUrl)
        assertEquals(FieldPatch.Unchanged, memorial.memorialVideo)
    }

    /** 선택 키는 화면 전용 식별자라 서버로 나가지 않는다 — 재조회로 값이 달라져도 변경이 아니다. */
    @Test
    fun `곡 선택 키가 달라도 곡 내용이 같으면 변경이 아니다`() {
        val updated =
            buildMemorialUpdate(
                playlistSongs =
                    listOf(Song(selectionKey = "search:가수|곡|0", title = "곡", artist = "가수", albumCoverUrl = null)),
            )

        assertNull(updated.memorial)
    }

    /** 사진만 지운 저장은 사진 슬롯만 삭제로 말하고, 곡·영상은 건드리지 않는다. */
    @Test
    fun `영정 사진만 지우면 사진 슬롯만 삭제로 나간다`() {
        val updated =
            buildMemorialUpdate(
                memorialMedia =
                    MemorialMediaUrls(
                        memorialVideoUrl = "https://cdn.test/afternotes/video.mp4",
                        memorialThumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                        memorialPhotoUrl = null,
                    ),
            )

        val memorial = requireNotNull(updated.memorial) { "사진을 지웠으므로 플레이리스트가 실려야 한다" }
        assertEquals(FieldPatch.Set(null), memorial.memorialPhotoUrl)
        assertNull("곡을 건드린 적이 없다", memorial.songs)
        assertEquals("영상을 건드린 적이 없다", FieldPatch.Unchanged, memorial.memorialVideo)
    }

    @Test
    fun `추모 영상만 바꾸면 영상 슬롯만 실린다`() {
        val updated =
            buildMemorialUpdate(
                memorialMedia =
                    MemorialMediaUrls(
                        memorialVideoUrl = "https://cdn.test/afternotes/new-video.mp4",
                        memorialThumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                        memorialPhotoUrl = "https://cdn.test/afternotes/photo.jpg",
                    ),
            )

        val memorial = requireNotNull(updated.memorial) { "영상을 바꿨으므로 플레이리스트가 실려야 한다" }
        assertEquals(
            FieldPatch.Set(
                MemorialVideoPayload(
                    videoUrl = "https://cdn.test/afternotes/new-video.mp4",
                    thumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                ),
            ),
            memorial.memorialVideo,
        )
        assertEquals("사진을 건드린 적이 없다", FieldPatch.Unchanged, memorial.memorialPhotoUrl)
        assertNull("곡을 건드린 적이 없다", memorial.songs)
    }
}
