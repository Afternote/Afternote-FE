package com.afternote.feature.timeletter.data.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeLetterDtoTest {
    @Test
    fun `nullable response fields may be omitted`() {
        val payload =
            """
            {
              "id": 55,
              "title": "fuf",
              "sendAt": "2026-08-28T21:39:00",
              "status": "DRAFT",
              "blocks": [
                {
                  "id": 79,
                  "blockType": "TEXT",
                  "blockOrder": 1,
                  "textContent": "q",
                  "url": null
                }
              ],
              "receiverIds": [13]
            }
            """.trimIndent()

        val result = Json.decodeFromString<TimeLetterDto>(payload)

        assertEquals(55L, result.id)
        assertNull(result.deliveredAt)
        assertNull(result.blocks.single().mimeType)
    }
}
