package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.testing.FakeReceiverTimeLetterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.ArrayDeque

/** 목록 재진입 갱신([RecipientTimeletterViewModel.refreshOnReturn]) 실패 시 화면 유지 계약 가드. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipientTimeletterViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshOnReturn - 실패해도 보고 있던 목록을 유지한다`() =
        runTest {
            val letters = ReceivedTimeLetterList(listOf(testLetter(id = 1L)), 1)
            val results =
                ArrayDeque(listOf(Result.success(letters), Result.failure<ReceivedTimeLetterList>(IOException("일시적 실패"))))
            val repository =
                FakeReceiverTimeLetterRepository().apply {
                    onGetReceivedTimeLetters = { results.removeFirst().getOrThrow() }
                }
            val viewModel = RecipientTimeletterViewModel(repository)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME — 여기서 실패

            assertTrue(states.last() is RecipientTimeletterUiState.Success)
            assertTrue(states.none { it is RecipientTimeletterUiState.Error })
        }

    private fun TestScope.recordStates(viewModel: RecipientTimeletterViewModel): List<RecipientTimeletterUiState> {
        val states = mutableListOf<RecipientTimeletterUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states += it }
        }
        return states
    }

    private companion object {
        fun testLetter(id: Long): ReceivedTimeLetter =
            ReceivedTimeLetter(
                id = id,
                timeLetterReceiverId = id,
                title = "제목",
                blocks = emptyList(),
                sendAt = null,
                status = TimeLetterStatus.SENT,
                senderName = "보낸이",
                deliveredAt = null,
                createdAt = null,
                isRead = false,
            )
    }
}
