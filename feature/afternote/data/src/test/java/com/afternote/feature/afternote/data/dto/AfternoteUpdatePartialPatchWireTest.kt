package com.afternote.feature.afternote.data.dto

import com.afternote.core.network.di.NetworkModule
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.FieldPatch
import com.afternote.feature.afternote.domain.model.author.MemorialPatchPayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 수정 요청 바디를 **JSON 문자열째** 고정한다 (#1617).
 *
 * DTO 객체만 단언하면 이 축의 결함이 통째로 빠져나간다 — 슬롯이 「키를 뺐는지」와 「null 을 실었는지」는
 * 직렬화 설정과 기본값의 함수라서, 객체 수준에서는 둘이 똑같아 보인다. 실제로 그렇게 새어 나갔다:
 * 기본값 없는 `memorialPhotoUrl` 이 `null` 이면 키를 달고 나가 삭제 지시가 됐다.
 *
 * 그래서 앱이 실제로 쓰는 [NetworkModule.provideJson] 으로 인코딩한 **문자열 전체**를 견준다.
 * 누가 `encodeDefaults` 를 켜거나 슬롯 기본값을 떼면 여기서 깨진다.
 */
class AfternoteUpdatePartialPatchWireTest {
    private val json = NetworkModule.provideJson()

    private fun wire(payload: AfternoteUpdatePayload): String = json.encodeToString(payload.toRequest())

    /**
     * 이슈 #1617 이 든 재현 경로의 반대편 — 제목만 고친 저장은 나머지를 **말하지 않는다.**
     * 키 하나라도 되살아나면 에디터를 연 뒤 서버가 바뀐 경우 그 필드가 낡은 값으로 덮인다.
     */
    @Test
    fun `제목만 실은 수정은 카테고리와 제목 두 키만 나간다`() {
        val body = wire(AfternoteUpdatePayload(type = AfternoteType.SOCIAL_NETWORK, title = "새 제목"))

        assertEquals("""{"category":"SOCIAL","title":"새 제목"}""", body)
    }

    /** 제목조차 안 건드렸으면 제목도 빠진다 — 서버가 `title` 생략을 계약으로 못박아 뒀다. */
    @Test
    fun `아무것도 안 실으면 카테고리만 나간다`() {
        val body = wire(AfternoteUpdatePayload(type = AfternoteType.MEMORIAL))

        assertEquals("""{"category":"PLAYLIST"}""", body)
    }

    /**
     * **곡만 고친 저장이 사진·영상을 지우면 안 된다.**
     *
     * 이 PR 이 처음 고쳤을 때 남아 있던 결함이 정확히 여기였다. `playlist` 를 한 덩어리로 판정해
     * 곡 변경만으로 세 슬롯을 함께 실었고, 기본값 없는 사진·영상 슬롯이 `null` 로 직렬화돼
     * `{"memorialPhotoUrl":null,"memorialVideo":null}` 이 삭제 지시로 나갔다.
     */
    @Test
    fun `곡만 바꾼 수정은 playlist 안에 songs 키만 남긴다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    memorial =
                        MemorialPatchPayload(
                            songs = listOf(MemorialSongPayload(title = "곡", artist = "가수", coverUrl = null)),
                        ),
                ),
            )

        assertEquals(
            """{"category":"PLAYLIST","playlist":{"songs":[{"title":"곡","artist":"가수"}]}}""",
            body,
        )
    }

    /** 곡을 전부 뺀 것은 삭제 지시다 — 빈 배열이 나가고, 미디어 슬롯은 여전히 빠져 있어야 한다. */
    @Test
    fun `곡을 전부 빼면 빈 배열만 나가고 미디어 키는 없다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    memorial = MemorialPatchPayload(songs = emptyList()),
                ),
            )

        assertEquals("""{"category":"PLAYLIST","playlist":{"songs":[]}}""", body)
    }

    /** 사진을 지운 저장만 사진 슬롯에 명시적 null 을 싣는다 — 곡·영상 키는 나가지 않는다. */
    @Test
    fun `영정 사진만 지우면 사진 슬롯에만 명시적 null 이 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    memorial = MemorialPatchPayload(memorialPhotoUrl = FieldPatch.Set(null)),
                ),
            )

        assertEquals("""{"category":"PLAYLIST","playlist":{"memorialPhotoUrl":null}}""", body)
    }

    @Test
    fun `추모 영상만 지우면 영상 슬롯에만 명시적 null 이 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    memorial = MemorialPatchPayload(memorialVideo = FieldPatch.Set(null)),
                ),
            )

        assertEquals("""{"category":"PLAYLIST","playlist":{"memorialVideo":null}}""", body)
    }

    @Test
    fun `미디어에 값을 실으면 종전과 같은 모양으로 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    memorial =
                        MemorialPatchPayload(
                            memorialPhotoUrl = FieldPatch.Set("https://cdn.test/afternotes/photo.jpg"),
                            memorialVideo =
                                FieldPatch.Set(
                                    MemorialVideoPayload(
                                        videoUrl = "https://cdn.test/afternotes/video.mp4",
                                        thumbnailUrl = "https://cdn.test/afternotes/thumb.jpg",
                                    ),
                                ),
                        ),
                ),
            )

        assertEquals(
            """{"category":"PLAYLIST","playlist":{"memorialPhotoUrl":"https://cdn.test/afternotes/photo.jpg",""" +
                """"memorialVideo":{"videoUrl":"https://cdn.test/afternotes/video.mp4",""" +
                """"thumbnailUrl":"https://cdn.test/afternotes/thumb.jpg"}}}""",
            body,
        )
    }

    /**
     * **비밀번호만 고친 저장이 아이디를 되돌리면 안 된다.**
     *
     * 서버는 `CredentialsRelationSupport.update` 에서 id·비밀번호를 각각 `!= null` 일 때만 갈아
     * 끼운다. 안 고친 아이디를 함께 실으면 그 사이 다른 기기가 바꾼 값이 낡은 값으로 되돌아간다.
     */
    @Test
    fun `비밀번호만 고치면 credentials 안에 password 키만 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.SOCIAL_NETWORK,
                    credentials = AfternoteAccountCredentials(password = "새 비밀번호"),
                ),
            )

        assertEquals("""{"category":"SOCIAL","credentials":{"password":"새 비밀번호"}}""", body)
    }

    @Test
    fun `아이디만 고치면 credentials 안에 id 키만 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.SOCIAL_NETWORK,
                    credentials = AfternoteAccountCredentials(id = "새 아이디"),
                ),
            )

        assertEquals("""{"category":"SOCIAL","credentials":{"id":"새 아이디"}}""", body)
    }

    /** 「전부 삭제」는 「안 건드림」과 다른 지시다 — 빈 배열이 키를 달고 나가야 서버가 지운다. */
    @Test
    fun `빈 목록은 생략되지 않고 빈 배열로 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.GALLERY_AND_FILES,
                    processingMethods = emptyList(),
                    leaveMessageBlocks = emptyList(),
                    receivers = emptyList(),
                ),
            )

        assertEquals(
            """{"category":"GALLERY","actions":[],"leaveMessage":[],"receivers":[]}""",
            body,
        )
    }

    @Test
    fun `값을 실은 필드는 종전과 같은 키로 그대로 나간다`() {
        val body =
            wire(
                AfternoteUpdatePayload(
                    type = AfternoteType.BUSINESS,
                    title = "제목",
                    processingMethods = listOf("계정 삭제"),
                    leaveMessageBlocks = listOf(LeaveMessageBlock(title = "가족에게", body = "고마웠어")),
                    credentials = AfternoteAccountCredentials(id = "account", password = "pw"),
                    receivers = listOf(ReceiverRefPayload(receiverId = 11L)),
                ),
            )

        assertEquals(
            """{"category":"BUSINESS","title":"제목","actions":["계정 삭제"],""" +
                """"leaveMessage":[{"title":"가족에게","body":"고마웠어"}],""" +
                """"credentials":{"id":"account","password":"pw"},"receivers":[{"receiverId":11}]}""",
            body,
        )
    }
}
