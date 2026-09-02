package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
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
