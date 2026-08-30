package com.afternote.feature.afternote.data.dto

import com.afternote.core.network.di.NetworkModule
import com.afternote.feature.afternote.data.mapper.toRequest
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 수정 요청 바디의 **키 유무**를 고정한다 (#1617).
 *
 * 서버는 「키 없음 = 기존 값 유지」로 읽으므로(Afternote-BE `bbff47c` · BE#200·#201), 사용자가 만지지
 * 않은 필드를 빼는 것이 곧 lost update 방지다. 어느 필드를 뺄지는 `AfternoteEditorFormMapper` 가
 * 정하고, **정말로 wire 에서 빠지는지**는 여기서 못박는다.
 *
 * [AfternotePlaylistRequestWireTest] 와 같은 이유로 테스트용 Json 을 새로 만들지 않고 앱이 실제로
 * 쓰는 [NetworkModule.provideJson] 을 그대로 태운다 — 이 계약은 값이 아니라 *직렬화 설정* 위에
 * 서 있어서, 누가 `encodeDefaults` 를 켜면 여기서 깨져야 한다.
 */
class AfternoteUpdatePartialPatchWireTest {
    private val json = NetworkModule.provideJson()

    private fun body(payload: AfternoteUpdatePayload) = json.encodeToJsonElement(payload.toRequest()).jsonObject

    /**
     * 이슈 #1617 이 든 재현 경로의 반대편이다 — 제목만 고친 저장은 나머지를 **말하지 않는다.**
     * 여기 어느 키 하나라도 되살아나면, 에디터를 연 뒤 서버가 바뀐 경우 그 필드가 낡은 값으로 덮인다.
     */
    @Test
    fun `제목만 실은 수정은 나머지 키가 바디에 아예 없다`() {
        val patch = body(AfternoteUpdatePayload(type = AfternoteType.SOCIAL_NETWORK, title = "새 제목"))

        assertEquals("새 제목", patch.getValue("title").jsonPrimitive.content)
        assertFalse("playlist" in patch)
        assertFalse("receivers" in patch)
        assertFalse("actions" in patch)
        assertFalse("leaveMessage" in patch)
        assertFalse("credentials" in patch)
    }

    /** 제목조차 안 건드렸으면 제목도 빠진다 — 서버가 `title` 생략을 계약으로 못박아 뒀다. */
    @Test
    fun `제목을 안 건드리면 title 키도 빠진다`() {
        val patch = body(AfternoteUpdatePayload(type = AfternoteType.SOCIAL_NETWORK))

        assertFalse("title" in patch)
    }

    /**
     * 카테고리만 늘 나간다 — 값을 바꾸는 필드가 아니라 **대상 확인용 단언**이다.
     * 서버는 저장값과 다르면 400 을 내고, 같으면 아무것도 바꾸지 않는다.
     */
    @Test
    fun `카테고리는 아무것도 안 고쳐도 늘 실린다`() {
        val patch = body(AfternoteUpdatePayload(type = AfternoteType.SOCIAL_NETWORK))

        assertEquals("SOCIAL", patch.getValue("category").jsonPrimitive.content)
    }

    /** 「전부 삭제」는 「안 건드림」과 다른 지시다 — 빈 배열이 키를 달고 나가야 서버가 지운다. */
    @Test
    fun `빈 목록은 생략되지 않고 빈 배열로 실린다`() {
        val patch =
            body(
                AfternoteUpdatePayload(
                    type = AfternoteType.SOCIAL_NETWORK,
                    processingMethods = emptyList(),
                    leaveMessageBlocks = emptyList(),
                    receivers = emptyList(),
                ),
            )

        assertTrue("actions" in patch)
        assertTrue(patch.getValue("actions").jsonArray.isEmpty())
        assertTrue("leaveMessage" in patch)
        assertTrue(patch.getValue("leaveMessage").jsonArray.isEmpty())
        assertTrue("receivers" in patch)
        assertTrue(patch.getValue("receivers").jsonArray.isEmpty())
    }

    @Test
    fun `값을 실은 필드는 종전과 같은 키로 그대로 나간다`() {
        val patch =
            body(
                AfternoteUpdatePayload(
                    type = AfternoteType.BUSINESS,
                    title = "제목",
                    processingMethods = listOf("계정 삭제"),
                    leaveMessageBlocks = listOf(LeaveMessageBlock(title = "가족에게", body = "고마웠어")),
                    credentials = AfternoteAccountCredentials(id = "account", password = "pw"),
                    receivers = listOf(ReceiverRefPayload(receiverId = 11L)),
                ),
            )

        assertEquals("BUSINESS", patch.getValue("category").jsonPrimitive.content)
        assertEquals(
            "계정 삭제",
            patch
                .getValue("actions")
                .jsonArray
                .single()
                .jsonPrimitive.content,
        )
        assertEquals(
            "고마웠어",
            patch
                .getValue("leaveMessage")
                .jsonArray
                .single()
                .jsonObject
                .getValue("body")
                .jsonPrimitive.content,
        )
        assertEquals(
            "account",
            patch
                .getValue("credentials")
                .jsonObject
                .getValue("id")
                .jsonPrimitive.content,
        )
        assertEquals(
            11L,
            patch
                .getValue("receivers")
                .jsonArray
                .single()
                .jsonObject
                .getValue("receiverId")
                .jsonPrimitive.content
                .toLong(),
        )
    }

    /**
     * 추억 노트의 미디어 삭제는 **명시적 null** 이라 [MemorialWritePayload] 를 실을 때만 나간다 (#1596).
     * 「안 건드림」은 그 바깥 — `playlist` 키 자체가 빠지는 것으로 말한다.
     */
    @Test
    fun `플레이리스트를 안 건드리면 미디어 삭제 지시도 나가지 않는다`() {
        val untouched = body(AfternoteUpdatePayload(type = AfternoteType.MEMORIAL, title = "새 제목"))
        val emptied =
            body(
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    memorial = MemorialWritePayload(memorialPhotoUrl = null, songs = emptyList(), memorialVideo = null),
                ),
            )

        assertFalse("playlist" in untouched)
        assertTrue("playlist" in emptied)
        assertTrue("memorialPhotoUrl" in emptied.getValue("playlist").jsonObject)
    }
}
