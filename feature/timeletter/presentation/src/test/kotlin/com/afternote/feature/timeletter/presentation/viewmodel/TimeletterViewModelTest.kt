package com.afternote.feature.timeletter.presentation.viewmodel

import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
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
import java.lang.reflect.Proxy

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
            timeLetterRepository { methodName, args ->
                when (methodName) {
                    "getTimeLetters" -> {
                        testLetters.also { loadCount += 1 }
                    }

                    "deleteTimeLetters" -> {
                        Unit.also {
                            deletedIds = (args?.first() as? List<*>)?.filterIsInstance<Long>()
                        }
                    }

                    else -> {
                        error("Unexpected repository call: $methodName")
                    }
                }
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
            timeLetterRepository { methodName, _ ->
                when (methodName) {
                    "getTimeLetters" -> testLetters
                    "deleteTimeLetters" -> throw IllegalStateException("delete failed")
                    else -> error("Unexpected repository call: $methodName")
                }
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
            timeLetterRepository { methodName, _ ->
                when (methodName) {
                    "getTimeLetters" -> throw IllegalStateException("load failed")
                    else -> error("Unexpected repository call: $methodName")
                }
            }
        val viewModel = TimeletterViewModel(repository, userRepository())

        viewModel.load()

        assertEquals(TimeletterUiState.Error, viewModel.uiState.value)
    }

    private fun timeLetterRepository(handler: (String, Array<out Any?>?) -> Any?): TimeLetterRepository =
        Proxy.newProxyInstance(
            TimeLetterRepository::class.java.classLoader,
            arrayOf(TimeLetterRepository::class.java),
        ) { _, method, args -> handler(method.name, args) } as TimeLetterRepository

    private fun userRepository(): UserRepository =
        Proxy.newProxyInstance(
            UserRepository::class.java.classLoader,
            arrayOf(UserRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                "getReceivers" -> emptyList<Any>()
                else -> error("Unexpected user repository call: ${method.name}")
            }
        } as UserRepository

    private fun overlappingDeleteAndReloadRepository(
        deleteResult: CompletableDeferred<Unit>,
        reloadResult: CompletableDeferred<TimeLetterList>,
    ): TimeLetterRepository =
        object : TimeLetterRepository {
            private var loadCount = 0

            override suspend fun getTimeLetters(): TimeLetterList = if (loadCount++ == 0) testLetters else reloadResult.await()

            override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
                deleteResult.await()
            }

            override suspend fun getTemporaryTimeLetters(): TimeLetterList = error("Unexpected call")

            override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter = error("Unexpected call")

            override suspend fun createTimeLetter(
                title: String?,
                blocks: List<NewTimeLetterBlock>,
                sendAt: String?,
                deliveryMode: TimeLetterDeliveryMode,
                status: TimeLetterStatus,
                receiverIds: List<Long>,
            ): TimeLetter = error("Unexpected call")

            override suspend fun updateTimeLetter(
                timeLetterId: Long,
                title: String?,
                blocks: List<NewTimeLetterBlock>,
                sendAt: String?,
                deliveryMode: TimeLetterDeliveryMode?,
                status: TimeLetterStatus?,
            ): TimeLetter = error("Unexpected call")

            override suspend fun deleteAllTemporary() = error("Unexpected call")
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
                            deliveredAt = null,
                            status = TimeLetterStatus.SCHEDULED,
                            blocks = emptyList(),
                            receiverIds = emptyList(),
                        ),
                    ),
                totalCount = 1,
            )
    }
}
