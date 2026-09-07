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
class DraftLetterViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `receiver load failure keeps drafts visible`() {
        val viewModel =
            DraftLetterViewModel(
                timeLetterRepository { methodName, _ ->
                    when (methodName) {
                        "getTemporaryTimeLetters" -> testDrafts
                        else -> error("Unexpected repository call: $methodName")
                    }
                },
                userRepository { throw IllegalStateException("receiver load failed") },
            )

        val state = viewModel.uiState.value as DraftLetterUiState.Success

        assertEquals(testDrafts.timeLetters, state.drafts)
        assertTrue(state.receiverNameMap.isEmpty())
    }

    @Test
    fun `duplicate delete requests are ignored while deletion is running`() {
        val deleteResult = CompletableDeferred<Unit>()
        var deleteCalls = 0
        val viewModel =
            DraftLetterViewModel(
                object : TimeLetterRepository {
                    override suspend fun getTemporaryTimeLetters(): TimeLetterList = testDrafts

                    override suspend fun deleteAllTemporary() {
                        deleteCalls += 1
                        deleteResult.await()
                    }

                    override suspend fun getTimeLetters(): TimeLetterList = unexpectedCall()

                    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter = unexpectedCall()

                    override suspend fun createTimeLetter(
                        title: String?,
                        blocks: List<NewTimeLetterBlock>,
                        sendAt: String?,
                        deliveryMode: TimeLetterDeliveryMode,
                        status: TimeLetterStatus,
                        receiverIds: List<Long>,
                    ): TimeLetter = unexpectedCall()

                    override suspend fun updateTimeLetter(
                        timeLetterId: Long,
                        title: String?,
                        blocks: List<NewTimeLetterBlock>,
                        sendAt: String?,
                        deliveryMode: TimeLetterDeliveryMode?,
                        status: TimeLetterStatus?,
                    ): TimeLetter = unexpectedCall()

                    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) = unexpectedCall<Unit>()
                },
                userRepository { emptyList<Any>() },
            )

        viewModel.deleteAll()
        viewModel.deleteAll()

        assertEquals(1, deleteCalls)
        assertTrue((viewModel.uiState.value as DraftLetterUiState.Success).isDeleting)

        deleteResult.complete(Unit)

        val state = viewModel.uiState.value as DraftLetterUiState.Success
        assertTrue(state.drafts.isEmpty())
        assertFalse(state.isDeleting)
    }

    @Test
    fun `delete failure keeps latest drafts and exposes message`() {
        val viewModel =
            DraftLetterViewModel(
                timeLetterRepository { methodName, _ ->
                    when (methodName) {
                        "getTemporaryTimeLetters" -> testDrafts
                        "deleteAllTemporary" -> throw IllegalStateException("delete failed")
                        else -> error("Unexpected repository call: $methodName")
                    }
                },
                userRepository { emptyList<Any>() },
            )

        viewModel.deleteAll()

        val state = viewModel.uiState.value as DraftLetterUiState.Success
        assertEquals(testDrafts.timeLetters, state.drafts)
        assertFalse(state.isDeleting)
        assertEquals(com.afternote.feature.timeletter.presentation.R.string.timeletter_draft_delete_error, state.messageRes)
    }

    @Test
    fun `delete selected is disabled when no draft is selected`() {
        val state = DraftLetterUiState.Success(drafts = testDrafts.timeLetters)

        assertFalse(state.isDeleteSelectedEnabled)
    }

    @Test
    fun `delete selected is enabled when a draft is selected`() {
        val state =
            DraftLetterUiState.Success(
                drafts = testDrafts.timeLetters,
                selectedIds = setOf(testDrafts.timeLetters.single().id),
            )

        assertTrue(state.isDeleteSelectedEnabled)
    }

    private fun timeLetterRepository(handler: (String, Array<out Any?>?) -> Any?): TimeLetterRepository =
        Proxy.newProxyInstance(
            TimeLetterRepository::class.java.classLoader,
            arrayOf(TimeLetterRepository::class.java),
        ) { _, method, args -> handler(method.name, args) } as TimeLetterRepository

    private fun userRepository(getReceivers: () -> Any): UserRepository =
        Proxy.newProxyInstance(
            UserRepository::class.java.classLoader,
            arrayOf(UserRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getReceiverListFlow" -> flowOf(emptyList<Any>())
                "getReceivers" -> getReceivers()
                else -> error("Unexpected user repository call: ${method.name}")
            }
        } as UserRepository

    private fun <T> unexpectedCall(): T = error("Unexpected repository call")

    private companion object {
        val testDrafts =
            TimeLetterList(
                timeLetters =
                    listOf(
                        TimeLetter(
                            id = 1L,
                            title = "draft",
                            sendAt = null,
                            deliveredAt = null,
                            status = TimeLetterStatus.DRAFT,
                            blocks = emptyList(),
                            receiverIds = listOf(1L),
                        ),
                    ),
                totalCount = 1,
            )
    }
}
