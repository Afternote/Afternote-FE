package com.afternote.feature.timeletter.domain.testing

import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeTimeLetterRepositoryTest {
    @Test
    fun `임시저장 편지를 예약 상태로 수정하면 등록 목록으로 이동한다`() =
        runBlocking {
            val draft = timeLetter(status = TimeLetterStatus.DRAFT)
            val repository =
                FakeTimeLetterRepository(
                    temporaryLetters = TimeLetterList(listOf(draft), totalCount = 1),
                )

            repository.updateTimeLetter(
                timeLetterId = draft.id,
                title = "예약 편지",
                blocks = emptyList(),
                sendAt = "2030-01-01T00:00:00",
                deliveryMode = null,
                status = TimeLetterStatus.SCHEDULED,
            )

            assertTrue(repository.temporaryLetters.timeLetters.isEmpty())
            assertEquals(listOf(draft.id), repository.registeredLetters.timeLetters.map(TimeLetter::id))
            assertEquals(TimeLetterStatus.SCHEDULED, repository.details.getValue(draft.id).status)
        }

    @Test
    fun `삭제 onX는 기본 메모리 변경을 대체하면서 호출은 기록한다`() =
        runBlocking {
            val letter = timeLetter(status = TimeLetterStatus.SCHEDULED)
            val repository =
                FakeTimeLetterRepository(
                    registeredLetters = TimeLetterList(listOf(letter), totalCount = 1),
                    onDeleteTimeLetters = { },
                )

            repository.deleteTimeLetters(listOf(letter.id))

            assertEquals(listOf(listOf(letter.id)), repository.deleteCalls)
            assertEquals(listOf(letter), repository.registeredLetters.timeLetters)
            assertEquals(letter, repository.details.getValue(letter.id))
        }

    private fun timeLetter(status: TimeLetterStatus): TimeLetter =
        TimeLetter(
            id = 1L,
            title = "편지",
            sendAt = null,
            status = status,
            blocks = emptyList(),
            receiverIds = listOf(7L),
        )
}
