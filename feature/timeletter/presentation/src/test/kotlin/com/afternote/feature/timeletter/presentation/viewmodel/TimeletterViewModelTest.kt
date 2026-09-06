package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimeletterViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `delete success reloads the time letter list`() {
        var loadCount = 0
        var deletedIds: List<Long>? = null
        val repository =
            FakeTimeLetterRepository.strict().apply {
                onGetTimeLetters = { testLetters.also { loadCount += 1 } }
                onDeleteTimeLetters = { deletedIds = it }
            }
        val viewModel = TimeletterViewModel(repository, userRepository())

        viewModel.load()
        viewModel.deleteTimeLetter(1L)

        assertEquals(listOf(1L), deletedIds)
        assertEquals(2, loadCount)
        val state = viewModel.uiState.value as TimeletterUiState.Success
        assertFalse(state.isDeleting)
    }

    @Test
    fun `delete failure keeps the list and exposes a consumable failure state`() {
        val repository =
            FakeTimeLetterRepository.strict().apply {
                onGetTimeLetters = { testLetters }
                onDeleteTimeLetters = { throw IllegalStateException("delete failed") }
            }
        val viewModel = TimeletterViewModel(repository, userRepository())

        viewModel.load()
        viewModel.deleteTimeLetter(1L)

        val failedState = viewModel.uiState.value as TimeletterUiState.Success
        assertEquals(testLetters, failedState.letters)
        assertFalse(failedState.isDeleting)
        assertTrue(failedState.showDeleteFailure)

        viewModel.consumeDeleteFailure()

        assertFalse((viewModel.uiState.value as TimeletterUiState.Success).showDeleteFailure)
    }

    @Test
    fun `delete failure restores success state when reload is in progress`() {
        val deleteResult = CompletableDeferred<Unit>()
        val reloadResult = CompletableDeferred<TimeLetterList>()
        val repository = overlappingDeleteAndReloadRepository(deleteResult, reloadResult)
        val viewModel = TimeletterViewModel(repository, userRepository())

        viewModel.load()
        viewModel.deleteTimeLetter(1L)
        viewModel.load()

        assertEquals(TimeletterUiState.Loading, viewModel.uiState.value)

        deleteResult.completeExceptionally(IllegalStateException("delete failed"))

        val failedState = viewModel.uiState.value as TimeletterUiState.Success
        assertEquals(testLetters, failedState.letters)
        assertFalse(failedState.isDeleting)
        assertTrue(failedState.showDeleteFailure)

        reloadResult.complete(testLetters)
    }

    @Test
    fun `load failure exposes error state`() {
        val repository =
            FakeTimeLetterRepository.strict().apply {
                onGetTimeLetters = { throw IllegalStateException("load failed") }
            }
        val viewModel = TimeletterViewModel(repository, userRepository())

        viewModel.load()

        assertEquals(TimeletterUiState.Error, viewModel.uiState.value)
    }

    private fun userRepository(): FakeUserRepository =
        FakeUserRepository.strict().apply {
            onReceiverListFlow = { flowOf(emptyList()) }
            onGetReceivers = { emptyList() }
        }

    private fun overlappingDeleteAndReloadRepository(
        deleteResult: CompletableDeferred<Unit>,
        reloadResult: CompletableDeferred<TimeLetterList>,
    ): FakeTimeLetterRepository {
        var loadCount = 0
        return FakeTimeLetterRepository.strict().apply {
            onGetTimeLetters = {
                if (loadCount++ == 0) testLetters else reloadResult.await()
            }
            onDeleteTimeLetters = { deleteResult.await() }
        }
    }

    private companion object {
        val testLetters =
            TimeLetterList(
                timeLetters =
                    listOf(
                        TimeLetter(
                            id = 1L,
                            title = "제목",
                            sendAt = null,
                            status = TimeLetterStatus.SCHEDULED,
                            blocks = emptyList(),
                            receiverIds = emptyList(),
                        ),
                    ),
                totalCount = 1,
            )
    }
}
