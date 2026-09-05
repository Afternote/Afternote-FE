package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.testing.FakeFileMetadataRepository
import com.afternote.feature.timeletter.domain.testing.FakeTimeLetterRepository
import com.afternote.feature.timeletter.domain.testing.FakeVoiceRecorderRepository
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
import org.junit.Assert.assertTrue
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

            assertEquals(0, repository.createCalls.size)
            assertEquals(TimeLetterWriteError.RecipientRequired, viewModel.uiState.value.error)
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

            assertEquals(0, repository.getTimeLettersCalls)
            assertEquals(0, repository.createCalls.size)
            assertEquals(TimeLetterWriteError.RecipientRequired, viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.registered)
        }

    @Test
    fun `임시저장 API 실패 - 성공 상태를 만들지 않고 안전한 일반 오류로 매핑`() =
        runTest {
            val repository =
                FakeTimeLetterRepository().apply {
                    onCreateTimeLetter = { throw Exception("internal SQL details") }
                }
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L))

            viewModel.saveDraft(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.createCalls.size)
            assertEquals(TimeLetterWriteError.SaveFailed, viewModel.uiState.value.error)
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

            assertEquals(listOf(1L, 2L), repository.createCalls.single().receiverIds)
        }

    @Test
    fun `등록 개수 조회 취소 - 사용자 오류로 변환하지 않음`() =
        runTest {
            val repository =
                FakeTimeLetterRepository().apply {
                    onGetTimeLetters = { throw CancellationException("cancelled") }
                }
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L))
            viewModel.setSendAt("2026-08-16")

            viewModel.register(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.getTimeLettersCalls)
            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.registered)
        }

    @Test
    fun `저장 취소 - isSaving을 false로 복구하고 오류로 변환하지 않음`() =
        runTest {
            val repository =
                FakeTimeLetterRepository().apply {
                    onCreateTimeLetter = { throw CancellationException("cancelled") }
                }
            val viewModel = viewModel(repository)
            viewModel.setRecipients(listOf(1L))

            viewModel.saveDraft(title = "제목", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.createCalls.size)
            assertFalse(viewModel.uiState.value.isSaving)
            assertNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.savedAsDraft)
        }

    @Test
    fun `draft edit checks the free plan limit before scheduling`() =
        runTest {
            val editingLetter = testEditingLetter(status = TimeLetterStatus.DRAFT)
            val repository =
                FakeTimeLetterRepository(
                    registeredLetters = TimeLetterList(emptyList(), totalCount = 3),
                    details = mapOf(editingLetter.id to editingLetter),
                )
            val viewModel = viewModel(repository, editingLetter.id)
            advanceUntilIdle()

            viewModel.register(title = "title", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(1, repository.getTimeLettersCalls)
            assertEquals(0, repository.updateCalls.size)
            assertTrue(viewModel.uiState.value.showFreePlanLimitPopup)
            assertFalse(viewModel.uiState.value.registered)
        }

    @Test
    fun `scheduled edit does not check the free plan limit again`() =
        runTest {
            val editingLetter = testEditingLetter(status = TimeLetterStatus.SCHEDULED)
            val repository =
                FakeTimeLetterRepository(
                    registeredLetters = TimeLetterList(emptyList(), totalCount = 3),
                    details = mapOf(editingLetter.id to editingLetter),
                )
            val viewModel = viewModel(repository, editingLetter.id)
            advanceUntilIdle()

            viewModel.register(title = "title", textContents = emptyMap())
            advanceUntilIdle()

            assertEquals(0, repository.getTimeLettersCalls)
            assertEquals(1, repository.updateCalls.size)
            assertFalse(viewModel.uiState.value.showFreePlanLimitPopup)
            assertTrue(viewModel.uiState.value.registered)
        }

    private fun viewModel(
        repository: FakeTimeLetterRepository,
        editingTimeLetterId: Long? = null,
    ): TimeLetterWriteViewModel {
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
            fileMetadataRepository = FakeFileMetadataRepository.strict(),
            voiceRecorderRepository = FakeVoiceRecorderRepository,
            savedStateHandle = SavedStateHandle(mapOf("timeLetterId" to editingTimeLetterId)),
        )
    }

    private fun testEditingLetter(status: TimeLetterStatus): TimeLetter =
        TimeLetter(
            id = 10L,
            title = "existing title",
            sendAt = "2026-08-29T19:30:00",
            deliveredAt = null,
            status = status,
            blocks = emptyList(),
            receiverIds = listOf(1L),
        )
}
