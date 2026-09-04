package com.afternote.feature.afternote.data.mapper

import com.afternote.core.network.di.NetworkModule
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 임시저장 여부가 요청 바디에 어떻게 실리는지 가드한다.
 *
 * 생성과 수정의 뜻이 다르다 — 생성은 `false` 가 «정식 등록» 이고, 수정은 **생략(null)이 «저장값 유지»** 다
 * (BE `AfternoteValidator.validatePublishRequirements` 가 요청에 없으면 엔티티 값을 본다).
 *
 * **그 뜻은 DTO 객체가 아니라 직렬화 결과 위에 서 있다.** `encodeDefaults = false` 라 기본값과 같은
 * 값은 키째 빠지므로, 생성의 `false` 와 수정의 `null` 은 «키 없음» 으로 같아지고 수정의 `false` 만
 * 키로 나간다. 그래서 아래 「와이어」 절이 [NetworkModule.provideJson] 으로 실제 JSON 을 만들어
 * 단언한다 — 누가 `encodeDefaults` 를 켜면 생성 경로가 조용히 `isDraft=false` 를 싣게 되므로
 * 객체 단언만으로는 안 잡힌다.
 */
class AfternoteDraftRequestTest {
    @Test
    fun `생성 기본값은 정식 등록이다`() {
        assertFalse(accountPayload().toSocialRequest().isDraft)
        assertFalse(accountPayload().toBusinessRequest().isDraft)
        assertFalse(galleryPayload().toRequest().isDraft)
        assertFalse(memorialPayload().toRequest().isDraft)
    }

    @Test
    fun `임시저장 생성은 isDraft 를 싣는다`() {
        assertTrue(accountPayload(isDraft = true).toSocialRequest().isDraft)
        assertTrue(accountPayload(isDraft = true).toBusinessRequest().isDraft)
        assertTrue(galleryPayload(isDraft = true).toRequest().isDraft)
        assertTrue(memorialPayload(isDraft = true).toRequest().isDraft)
    }

    @Test
    fun `수정은 값을 안 정하면 isDraft 를 안 싣는다 - 서버가 저장값을 유지한다`() {
        assertNull(updatePayload().toRequest().isDraft)
    }

    @Test
    fun `수정으로 정식 등록 전환은 false 를 명시한다`() {
        assertEquals(false, updatePayload(isDraft = false).toRequest().isDraft)
    }

    @Test
    fun `수정으로 임시저장 유지는 true 를 싣는다`() {
        assertEquals(true, updatePayload(isDraft = true).toRequest().isDraft)
    }

    // ---- 와이어: 키가 나가느냐 마느냐가 곧 의미다 ----

    private val json = NetworkModule.provideJson()

    @Test
    fun `와이어 - 생성 기본값은 isDraft 키를 아예 안 싣는다`() {
        val body = json.encodeToJsonElement(galleryPayload().toRequest()).jsonObject

        assertFalse("생성 기본값은 encodeDefaults=false 로 키째 빠져야 한다: $body", "isDraft" in body)
    }

    @Test
    fun `와이어 - 임시저장 생성은 isDraft true 를 싣는다`() {
        val body = json.encodeToJsonElement(galleryPayload(isDraft = true).toRequest()).jsonObject

        assertEquals("true", body.getValue("isDraft").jsonPrimitive.content)
    }

    @Test
    fun `와이어 - 수정에서 값을 안 정하면 isDraft 키가 없다 - 서버가 저장값을 유지한다`() {
        val body = json.encodeToJsonElement(updatePayload().toRequest()).jsonObject

        assertFalse("키가 나가면 서버가 «유지» 대신 그 값으로 덮는다: $body", "isDraft" in body)
    }

    @Test
    fun `와이어 - 수정으로 정식 등록 전환은 isDraft false 를 키로 싣는다`() {
        val body = json.encodeToJsonElement(updatePayload(isDraft = false).toRequest()).jsonObject

        assertTrue("false 가 키째 빠지면 발행 전환이 «유지» 로 흡수된다: $body", "isDraft" in body)
        assertEquals("false", body.getValue("isDraft").jsonPrimitive.content)
    }

    private fun accountPayload(isDraft: Boolean = false) =
        CreateAccountPayload(title = "t", processingMethods = emptyList(), isDraft = isDraft)

    private fun galleryPayload(isDraft: Boolean = false) =
        CreateGalleryPayload(title = "t", processingMethods = emptyList(), isDraft = isDraft)

    private fun memorialPayload(isDraft: Boolean = false) =
        CreateMemorialPayload(
            title = "t",
            memorial = MemorialWritePayload(memorialPhotoUrl = null, songs = emptyList(), memorialVideo = null),
            isDraft = isDraft,
        )

    private fun updatePayload(isDraft: Boolean? = null) =
        AfternoteUpdatePayload(type = AfternoteType.SOCIAL_NETWORK, title = "t", isDraft = isDraft)
}
