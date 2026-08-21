package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import com.afternote.core.model.user.Receiver
import com.afternote.core.model.user.ReceiverCreated
import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.model.user.User
import com.afternote.core.model.user.UserConnectedAccount
import com.afternote.core.model.user.UserPushSetting
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
import kotlinx.coroutines.flow.Flow
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
        val resolveUseCase = ResolveTimeLetterBlocksUseCase(FakePhotoUploadRepository)
        return TimeLetterWriteViewModel(
            createTimeLetterUseCase = CreateTimeLetterUseCase(repository, resolveUseCase),
            resolveTimeLetterBlocksUseCase = resolveUseCase,
            timeLetterRepository = repository,
            userRepository = FakeUserRepository,
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

private object FakePhotoUploadRepository : PhotoUploadRepository {
    override suspend fun upload(
        uriString: String,
        directory: String,
    ): Result<String> = error("upload should not be called")
}

private object FakeFileMetadataRepository : FileMetadataRepository {
    override suspend fun getFileName(uriString: String): String = error("getFileName should not be called")

    override suspend fun getMimeType(uriString: String): String? = error("getMimeType should not be called")
}

private object FakeUserRepository : UserRepository {
    override val receiverListFlow: Flow<List<Receiver>> = flowOf(emptyList())

    override suspend fun getReceivers(): List<Receiver> = emptyList()

    override suspend fun createReceiver(
        name: String,
        relation: String,
        phone: String?,
        email: String?,
        message: String?,
    ): ReceiverCreated = error("createReceiver should not be called")

    override suspend fun getReceiverDetail(receiverId: Long): ReceiverDetail = error("getReceiverDetail should not be called")

    override suspend fun updateReceiver(
        receiverId: Long,
        name: String,
        phone: String,
        relation: String,
        email: String,
    ): Receiver = error("updateReceiver should not be called")

    override suspend fun updateReceiverMessage(
        receiverId: Long,
        message: String,
    ) = error("updateReceiverMessage should not be called")

    override suspend fun getMyProfile(): User = error("getMyProfile should not be called")

    override suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User = error("updateMyProfile should not be called")

    override suspend fun deleteAccount() = error("deleteAccount should not be called")

    override suspend fun logActivity() = error("logActivity should not be called")

    override suspend fun getMyPushSettings(): UserPushSetting = error("getMyPushSettings should not be called")

    override suspend fun updateMyPushSettings(
        timeLetter: Boolean?,
        mindRecord: Boolean?,
        afterNote: Boolean?,
    ): UserPushSetting = error("updateMyPushSettings should not be called")

    override suspend fun getConnectedAccounts(): UserConnectedAccount = error("getConnectedAccounts should not be called")

    override suspend fun linkConnectedAccount(
        provider: String,
        accessToken: String,
    ): UserConnectedAccount = error("linkConnectedAccount should not be called")

    override suspend fun unlinkConnectedAccount(provider: String): UserConnectedAccount =
        error("unlinkConnectedAccount should not be called")

    override suspend fun getReceiverDeliveryConditions(receiverId: Long): ReceiverDeliveryConditions =
        error("getReceiverDeliveryConditions should not be called")

    override suspend fun updateReceiverDeliveryConditions(
        receiverId: Long,
        conditions: List<DeliveryConditionItem>,
    ): ReceiverDeliveryConditions = error("updateReceiverDeliveryConditions should not be called")
}
