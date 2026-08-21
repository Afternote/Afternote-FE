package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AfternoteUpdatePayloadMapperTest {
    @Test
    fun `도메인 종류는 data 경계에서 작성 API category로 변환된다`() {
        val expectedByType =
            mapOf(
                AfternoteType.SOCIAL_NETWORK to "SOCIAL",
                AfternoteType.BUSINESS to "BUSINESS",
                AfternoteType.GALLERY_AND_FILES to "GALLERY",
                AfternoteType.MEMORIAL to "PLAYLIST",
            )

        expectedByType.forEach { (type, expected) ->
            assertEquals(expected, AfternoteUpdatePayload(type = type, title = "title").toRequest().type)
        }
    }

    @Test
    fun `저장 미지원 종류는 wire 요청으로 만들지 않는다`() {
        val result = runCatching { AfternoteUpdatePayload(type = AfternoteType.ESTATE, title = "title").toRequest() }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
