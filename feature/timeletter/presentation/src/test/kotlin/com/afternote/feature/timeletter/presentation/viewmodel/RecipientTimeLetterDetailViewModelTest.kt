package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetter
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.ArrayDeque

/** 상세 재진입 갱신([RecipientTimeLetterDetailViewModel.refreshOnReturn]) 실패 시 화면 유지 계약 가드. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecipientTimeLetterDetailViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshOnReturn - 실패해도 보고 있던 상세를 유지한다`() =
        runTest {
            val letter = testLetter(id = 1L)
            val results =
                ArrayDeque(listOf(Result.success(letter), Result.failure<ReceivedTimeLetter>(IOException("일시적 실패"))))
            val repository =
                FakeReceiverTimeLetterRepository().apply {
                    onGetReceivedTimeLetterDetail = { results.removeFirst().getOrThrow() }
                }
            val viewModel =
                RecipientTimeLetterDetailViewModel(
                    receiverTimeLetterRepository = repository,
                    savedStateHandle = SavedStateHandle(mapOf("timeLetterReceiverId" to letter.timeLetterReceiverId)),
                )
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME — 여기서 실패

            assertTrue(states.last() is RecipientTimeLetterDetailUiState.Success)
            assertTrue(states.none { it is RecipientTimeLetterDetailUiState.Error })
        }

    private fun TestScope.recordStates(viewModel: RecipientTimeLetterDetailViewModel): List<RecipientTimeLetterDetailUiState> {
        val states = mutableListOf<RecipientTimeLetterDetailUiState>()
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
