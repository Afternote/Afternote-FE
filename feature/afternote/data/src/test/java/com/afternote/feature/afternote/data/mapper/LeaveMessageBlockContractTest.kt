package com.afternote.feature.afternote.data.mapper

import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.LeaveMessageBlockDto
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.data.mapper.response.toDomain
import com.afternote.feature.receiver.data.mapper.toReceiverDomainList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 서버 `leaveMessage` 블록 배열 계약 회귀 가드 (이슈 #509).
 *
 * 계약이 단일 문자열에서 `{title, body}` 배열로 바뀌었을 때 DTO 가 `String?` 으로 남아 있어
 * 상세·수신 목록이 통째로 역직렬화에 실패했다. 파싱은 앱과 같은 [Json] 설정
 * (`NetworkModule.provideJson`)으로 검증한다.
 */
class LeaveMessageBlockContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    /**
     * 본문은 실서버 응답 전문 그대로다 (`GET /afternotes/29`, 2026-08-07 실측).
     * 제목을 비워 보낸 블록이 `title: null` 로 돌아온다는 점이 이 케이스의 요지다.
     *
     * `isDraft` 는 캡처 이후 서버가 추가한 필드라 여기서만 보강했다.
     */
    @Test
    fun `작성자 상세 - leaveMessage 배열을 파싱해 블록으로 옮긴다`() {
        val response =
            """
            {"afternoteId":29,"category":"SOCIAL","title":"509검증-배열","isDraft":false,"actions":["게시물 내리기"],
             "leaveMessage":[{"title":"가족에게","body":"잘 부탁해"},{"title":null,"body":"제목 없는 블록"}],
             "credentials":{"id":"qa","password":"qa"},
             "receivers":[{"receiverId":7,"name":"QA407Receiver","relation":"DAUGHTER"}],
             "playlist":null,"updatedAt":"2026-08-07T06:21:14.553567"}
            """.trimIndent()

        val detail = json.decodeFromString<AfternoteDetailDto>(response).toDomain()

        assertEquals(
            listOf(
                LeaveMessageBlock(title = "가족에게", body = "잘 부탁해"),
                LeaveMessageBlock(title = null, body = "제목 없는 블록"),
            ),
            detail.leaveMessageBlocks,
        )
    }

    /** 계약 변경 전 등록분은 서버 컨버터가 `[{title:"", body:원문}]` 으로 감싸 돌려준다 (`GET /afternotes/11` 실측). */
    @Test
    fun `작성자 상세 - 레거시 단일 문자열도 블록으로 감싸져 온다`() {
        val detail =
            json
                .decodeFromString<AfternoteDetailDto>(
                    """{"afternoteId":11,"category":"SOCIAL","title":"t","isDraft":false,"updatedAt":"2026-08-07T06:21:14.553567","receivers":[],"credentials":{"id":"qa","password":"qa"},"leaveMessage":[{"title":"","body":"재현용 남기실 말씀"}]}""",
                ).toDomain()

        assertEquals(
            listOf(LeaveMessageBlock(title = "", body = "재현용 남기실 말씀")),
            detail.leaveMessageBlocks,
        )
    }

    @Test
    fun `작성자 상세 - leaveMessage 가 없으면 빈 목록`() {
        val detail =
            json
                .decodeFromString<AfternoteDetailDto>(
                    """{"afternoteId":1,"category":"SOCIAL","title":"t","isDraft":false,"updatedAt":"2026-08-07T06:21:14.553567","receivers":[],"credentials":{"id":"qa","password":"qa"},"leaveMessage":null}""",
                ).toDomain()

        assertTrue(detail.leaveMessageBlocks.isEmpty())
    }

    /** 수신 목록은 응답에 `leaveMessage` 가 실려, 블록이 있는 항목 하나만 섞여도 목록 전체가 실패했다. */
    @Test
    fun `수신 목록 - 블록이 있는 항목이 섞여도 목록 전체가 파싱된다`() {
        val response =
            """
            {"afternotes":[
              {"id":1,"title":"인스타그램","category":"SOCIAL","leaveMessage":[{"title":"","body":"남긴 말"}]},
              {"id":2,"title":"사진첩","category":"GALLERY","leaveMessage":null}],
             "totalCount":2}
            """.trimIndent()

        val items =
            json
                .decodeFromString<ReceivedAfternoteListDto>(response)
                .afternotes
                .toReceiverDomainList(NoopErrorReporter)

        assertEquals(listOf(1L, 2L), items.map { it.id })
    }

    @Test
    fun `수신 상세 - leaveMessage 배열을 파싱해 블록으로 옮긴다`() {
        val detail =
            json
                .decodeFromString<ReceivedAfternoteDetailDto>(
                    """{"id":1,"category":"GALLERY","title":"사진첩","senderName":"이발신","actions":null,"leaveMessage":[{"title":null,"body":"사진은 남겨줘"}]}""",
                ).toDomain()

        assertEquals(
            listOf(LeaveMessageBlock(title = null, body = "사진은 남겨줘")),
            detail.leaveMessageBlocks,
        )
    }

    @Test
    fun `응답 매핑 - 본문이 비었거나 없는 블록은 버린다`() {
        val blocks =
            listOf(
                LeaveMessageBlockDto(title = "제목만", body = null),
                LeaveMessageBlockDto(title = "공백만", body = "   "),
                LeaveMessageBlockDto(title = null, body = "살아남는 본문"),
            ).toLeaveMessageBlocks()

        assertEquals(listOf(LeaveMessageBlock(title = null, body = "살아남는 본문")), blocks)
    }

    /** 서버가 문자열 `leaveMessage` 를 400 으로 거절하므로 요청은 반드시 배열이어야 한다. */
    @Test
    fun `요청 직렬화 - 블록 배열로 나간다`() {
        val request =
            AfternoteCreateAccountRequestDto(
                category = "SOCIAL",
                title = "인스타그램",
                processingMethods = listOf("게시물 내리기"),
                leaveMessage =
                    listOf(LeaveMessageBlock(title = "가족에게", body = "잘 부탁해")).toDto(),
                receivers = emptyList(),
            )

        val encoded = json.encodeToString(AfternoteCreateAccountRequestDto.serializer(), request)

        assertTrue(encoded.contains("\"category\":\"SOCIAL\""))
        assertTrue(!encoded.contains("\"type\""))
        assertTrue(encoded.contains(""""leaveMessage":[{"title":"가족에게","body":"잘 부탁해"}]"""))
    }

    @Test
    fun `요청 직렬화 - 블록이 없으면 필드를 싣지 않는다`() {
        assertNull(emptyList<LeaveMessageBlock>().toDto())
    }

    /** PLAYLIST 생성 요청만 `leaveMessage` 가 빠져 입력이 조용히 버려지던 회귀 가드 (이슈 #678). */
    @Test
    fun `요청 직렬화 - PLAYLIST 생성도 남기실 말씀 블록을 싣는다`() {
        val request =
            CreateMemorialPayload(
                title = "추억 노트",
                memorial = MemorialWritePayload(memorialPhotoUrl = null, songs = emptyList(), memorialVideo = null),
                leaveMessageBlocks = listOf(LeaveMessageBlock(title = "가족에게", body = "노래 들으며 기억해줘")),
            ).toRequest()

        val encoded = json.encodeToString(AfternoteCreatePlaylistRequestDto.serializer(), request)

        assertTrue(encoded.contains("\"category\":\"PLAYLIST\""))
        assertTrue(encoded.contains(""""leaveMessage":[{"title":"가족에게","body":"노래 들으며 기억해줘"}]"""))
    }
}

private object NoopErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}
