package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 수정 요청이 **사용자가 만진 필드만** 싣는지 고정한다 (#1617).
 *
 * 종전에는 폼이 든 전체 스냅샷이 매번 통째로 나갔다. 에디터를 연 뒤 서버가 바뀌면, 사용자가 건드린
 * 적도 없는 필드가 낡은 로컬 값으로 덮였다 — 곡을 만진 적 없는 「제목만 수정」이 남이 추가한 곡을
 * 전부 지우는 lost update 다.
 *
 * 여기서 고정하는 축은 둘이다.
 * 1. 안 건드린 필드는 페이로드에서 `null` 이 된다 → 요청에서 키째 빠진다 → 서버가 「유지」로 읽는다.
 * 2. **비운 것은 안 건드린 것과 다르다** — 빈 목록은 그대로 실려 「전부 삭제」로 나간다
 *    (#1596·#1599 가 세운 계약).
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
        baseline: AfternoteUpdatePayload? = AfternoteEditorFormMapper.buildUpdateBaseline(socialDetail),
    ) = AfternoteEditorFormMapper.buildUpdatePayload(
        type = AfternoteType.SOCIAL_NETWORK,
        payload = payload,
        selectedReceiverIds = selectedReceiverIds,
        playlistSongs = emptyList(),
        memorialMedia = MemorialMediaUrls(),
        baseline = baseline,
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

    /**
     * 「전부 삭제」와 「안 건드림」이 갈린다 — 빈 목록은 접히지 않고 그대로 실려야 한다.
     * 접어 버리면 사용자가 전부 지운 저장이 서버에 반영되지 않는다.
     */
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

    @Test
    fun `남기실 말씀을 전부 빼면 빈 목록이 실려 전부 삭제로 나간다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(messageBlocks = emptyList()))

        assertEquals(emptyList<LeaveMessageBlock>(), updated.leaveMessageBlocks)
    }

    @Test
    fun `계정 정보를 고치면 계정 정보가 실린다`() {
        val updated = buildSocialUpdate(payload = untouchedSocialPayload(password = "새 비밀번호"))

        assertEquals("새 비밀번호", updated.credentials?.password)
        assertNull("제목은 그대로다", updated.title)
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

    /** 기준이 없으면(상세 로드 실패 등) 덜 보내다 사용자의 편집을 잃느니 종전처럼 전량을 싣는다. */
    @Test
    fun `기준 스냅샷이 없으면 종전처럼 전량을 싣는다`() {
        val updated = buildSocialUpdate(baseline = null)

        assertEquals("인스타그램", updated.title)
        assertEquals(listOf("계정 삭제", "사진 백업"), updated.processingMethods)
        assertNotNull(updated.leaveMessageBlocks)
        assertNotNull(updated.credentials)
        assertEquals(2, updated.receivers?.size)
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

    @Test
    fun `곡을 전부 빼면 플레이리스트가 빈 곡 목록과 함께 실린다`() {
        val updated = buildMemorialUpdate(playlistSongs = emptyList())

        val memorial = requireNotNull(updated.memorial) { "곡을 지웠으므로 플레이리스트가 실려야 한다" }
        assertTrue("곡을 지운 것은 명시적 삭제다", memorial.songs.isEmpty())
        assertEquals("같이 실리는 사진은 종전 값 그대로다", "https://cdn.test/afternotes/photo.jpg", memorial.memorialPhotoUrl)
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

    @Test
    fun `영정 사진을 지우면 플레이리스트가 실려 삭제를 말한다`() {
        val updated =
            buildMemorialUpdate(
                memorialMedia =
                    MemorialMediaUrls(
                        memorialVideoUrl = "https://cdn.test/afternotes/video.mp4",
                        memorialThumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                        memorialPhotoUrl = null,
                    ),
            )

        assertNotNull(updated.memorial)
        assertNull("빈 사진은 명시적 null 로 나가 삭제가 된다", updated.memorial?.memorialPhotoUrl)
    }
}
