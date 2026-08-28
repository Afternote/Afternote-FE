package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TimeLetterWriteViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `수신자 없는 임시저장 - 생성 API를 호출하지 않고 필수 안내를 표시`() =
        runTest {
            val repository = FakeTimeLetterRepository()
            val viewModel = viewModel(repository)

            viewModel.saveDraft(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(0, repository.createCallCount)
            assertEquals(TimeLetterWriteError.RECIPIENT_REQUIRED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.savedAsDraft)
        }

    @Test
    fun `수신자 없는 정식 등록 - 조회와 생성 API를 호출하지 않고 필수 안내를 표시`() =
        runTest {
            val repository = FakeTimeLetterRepository()
            val viewModel = viewModel(repository)
            viewModel.setSendAt("2026-08-18")

            viewModel.register(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(0, repository.listCallCount)
            assertEquals(0, repository.createCallCount)
            assertEquals(TimeLetterWriteError.RECIPIENT_REQUIRED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.registered)
        }

    @Test
    fun `임시저장 API 실패 - 성공 상태를 만들지 않고 안전한 일반 오류로 매핑`() =
        runTest {
            val repository = FakeTimeLetterRepository(createFailure = Exception("internal SQL details"))
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L))

            viewModel.saveDraft(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.createCallCount)
            assertEquals(TimeLetterWriteError.SAVE_FAILED, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.savedAsDraft)
        }

    @Test
    fun `선택한 수신자 ID 목록은 null 변환 없이 생성 경계까지 전달된다`() =
        runTest {
            val repository = FakeTimeLetterRepository()
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L, 2L))

            viewModel.saveDraft(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(listOf(1L, 2L), repository.lastReceiverIds)
        }

    @Test
    fun `등록 개수 조회 취소 - 사용자 오류로 변환하지 않음`() =
        runTest {
            val repository = FakeTimeLetterRepository(listFailure = CancellationException("cancelled"))
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L))
            viewModel.setSendAt("2026-08-16")

            viewModel.register(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.listCallCount)
            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.registered)
        }

    @Test
    fun `저장 취소 - isSaving을 false로 복구하고 오류로 변환하지 않음`() =
        runTest {
            val repository = FakeTimeLetterRepository(createFailure = CancellationException("cancelled"))
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L))

            viewModel.saveDraft(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.createCallCount)
            assertFalse(viewModel.uiState.value.isSaving)
            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.savedAsDraft)
        }

    private fun viewModel(repository: FakeTimeLetterRepository): TimeLetterWriteViewModel {
        val resolveUseCase = ResolveTimeLetterBlocksUseCase(FakePhotoUploadRepository.strict())
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolveUseCase),
            resolveTimeLetterBlocksUseCase = resolveUseCase,
            timeLetterRepository = repository,
            userRepository =
                FakeUserRepository.strict().apply {
                    onReceiverListFlow = { flowOf(emptyList()) }
                    onGetReceivers = { emptyList() }
                },
            fileMetadataRepository = FakeFileMetadataRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to null)),
        )
    }
}

private class FakeTimeLetterRepository(
    private val createFailure: Throwable? = null,
    private val listFailure: Throwable? = null,
) : TimeLetterRepository {
    var createCallCount: Int = 0
        private set

    var listCallCount: Int = 0
        private set

    var lastReceiverIds: List<Long>? = null
        private set

    override suspend fun getTemporaryTimeLetters(): TimeLetterList = TimeLetterList(emptyList(), 0)

    override suspend fun createTimeLetter(
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode,
        status: TimeLetterStatus,
        receiverIds: List<Long>,
    ): TimeLetter {
        createCallCount++
        lastReceiverIds = receiverIds
        createFailure?.let { throw it }
        return TimeLetter(1L, title, sendAt, null, status, emptyList(), receiverIds)
    }

    override suspend fun getTimeLetters(): TimeLetterList {
        listCallCount++
        listFailure?.let { throw it }
        return TimeLetterList(emptyList(), 0)
    }

    override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter = error("getTimeLetter should not be called")

    override suspend fun updateTimeLetter(
        timeLetterId: Long,
        title: String?,
        blocks: List<NewTimeLetterBlock>,
        sendAt: String?,
        deliveryMode: TimeLetterDeliveryMode?,
        status: TimeLetterStatus?,
    ): TimeLetter = error("updateTimeLetter should not be called")

    override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) = error("deleteTimeLetters should not be called")

    override suspend fun deleteAllTemporary() = error("deleteAllTemporary should not be called")
}

private object FakeFileMetadataRepository : FileMetadataRepository {
    override suspend fun getFileName(uriString: String): String = error("getFileName should not be called")

    override suspend fun getMimeType(uriString: String): String? = error("getMimeType should not be called")
}
