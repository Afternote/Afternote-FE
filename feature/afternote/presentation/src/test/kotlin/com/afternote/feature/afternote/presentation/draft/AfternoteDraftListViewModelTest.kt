package com.afternote.feature.afternote.presentation.draft

import com.afternote.feature.afternote.domain.testing.FakeAfternoteRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이 화면은 임시저장만 보여야 한다.
 *
 * 서버가 발행분과 임시저장을 한 요청에 섞어 주지 않으므로(`draftOnly` 미전송 = 발행분만) 발행 목록을
 * 재활용하면 «임시저장 목록에 발행분이 뜨는» 결함이 된다 — 조회 자체가 갈렸는지로 닫는다.
 */
class AfternoteDraftListViewModelTest {
    @Test
    fun `임시저장 목록만 조회하고 발행 목록은 건드리지 않는다`() {
        val repository = FakeAfternoteRepository()

        AfternoteDraftListViewModel(repository)

        assertEquals(listOf<Any?>(null), repository.requestedDraftTypes.toList())
        assertTrue(repository.requestedTypes.isEmpty())
    }
}
