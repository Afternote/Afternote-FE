package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.Receiver
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
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

/** 상세 재진입 갱신([TimeLetterDetailViewModel.refreshOnReturn]) 실패 시 화면 유지 계약 가드. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimeLetterDetailViewModelTest {
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
            val results = ArrayDeque(listOf(Result.success(letter), Result.failure<TimeLetter>(IOException("일시적 실패"))))
            val repository =
                FakeTimeLetterRepository().apply {
                    onGetTimeLetter = { results.removeFirst().getOrThrow() }
                }
            val viewModel = viewModel(repository, timeLetterId = letter.id)
            val states = recordStates(viewModel)

            viewModel.refreshOnReturn() // 첫 진입의 ON_RESUME — 스킵
            viewModel.refreshOnReturn() // 백스택 복귀의 ON_RESUME — 여기서 실패

            assertTrue(states.last() is TimeLetterDetailUiState.Success)
            assertTrue(states.none { it is TimeLetterDetailUiState.Error })
        }

    private fun TestScope.recordStates(viewModel: TimeLetterDetailViewModel): List<TimeLetterDetailUiState> {
        val states = mutableListOf<TimeLetterDetailUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { states += it }
        }
        return states
    }

    private fun viewModel(
        repository: FakeTimeLetterRepository,
        timeLetterId: Long,
        userRepository: FakeUserRepository =
            FakeUserRepository(receivers = listOf(Receiver(1L, "수신자", "가족", "auth-1"))),
    ): TimeLetterDetailViewModel =
        TimeLetterDetailViewModel(
            timeLetterRepository = repository,
            userRepository = userRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to timeLetterId)),
        )

    private companion object {
        fun testLetter(id: Long): TimeLetter =
            TimeLetter(
                id = id,
                title = "제목",
                sendAt = null,
                deliveredAt = null,
                status = TimeLetterStatus.SENT,
                blocks = emptyList(),
                receiverIds = listOf(1L),
            )
    }
}
