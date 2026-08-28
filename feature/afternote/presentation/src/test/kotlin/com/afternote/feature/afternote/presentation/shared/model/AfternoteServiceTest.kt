package com.afternote.feature.afternote.presentation.shared.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AfternoteServiceTest {
    @Test
    fun `추억 노트는 현행 MEMORIAL 상수로 조회된다`() {
        assertEquals("추억 노트", AfternoteService.MEMORIAL.displayKey)
        assertSame(
            AfternoteService.MEMORIAL,
            AfternoteService.fromDisplayKeyOrNull("추억 노트"),
        )
    }
}
