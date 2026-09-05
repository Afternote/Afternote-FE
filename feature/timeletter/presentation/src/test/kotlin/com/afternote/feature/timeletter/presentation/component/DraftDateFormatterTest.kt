package com.afternote.feature.timeletter.presentation.component

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftDateFormatterTest {
    @Test
    fun `ISO date-time is formatted for the draft list`() {
        assertEquals("2026.07.31.", formatDraftSendAt("2026-07-31T00:00:00"))
    }

    @Test
    fun `date-only value is formatted for the draft list`() {
        assertEquals("2029.11.20.", formatDraftSendAt("2029-11-20"))
    }

    @Test
    fun `missing or malformed value uses dash`() {
        assertEquals("–", formatDraftSendAt(null))
        assertEquals("–", formatDraftSendAt(""))
        assertEquals("–", formatDraftSendAt("not-a-date"))
    }
}
