package com.afternote.feature.setting.presentation.profile

import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditMviTest {
    private val updateGates = ArrayDeque<CompletableDeferred<Result<User>>>()

    private val repository =
        FakeMyProfileRepository(
            profile = PROFILE,
            onUpdateMyProfile = { _, _, _ -> updateGates.removeFirst().await().getOrThrow() },
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load fills form and load failure becomes error`() {
        assertEquals(
            ProfileEditUiState.Success(name = "홍길동", phone = "", email = "hong@afternote.local"),
            ProfileEditViewModel(repository, FakePhotoUploadRepository.strict()).uiState.value,
        )

        val failing = FakeMyProfileRepository(onGetMyProfile = { error("load failed") })
        assertEquals(ProfileEditUiState.Error, ProfileEditViewModel(failing, FakePhotoUploadRepository.strict()).uiState.value)
    }

    @Test
    fun `update while in flight or after success does not submit again`() {
        val gate = enqueueUpdate()
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())

        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = ""))
        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "또 이름", phone = ""))

        assertTrue(viewModel.success().isUpdating)
        assertEquals(
            listOf(FakeMyProfileRepository.ProfileUpdateCall(name = "새 이름", phone = null, profileImageUrl = null)),
            repository.profileUpdateCalls,
        )

        gate.complete(Result.success(PROFILE))
        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "또 이름", phone = ""))

        assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        assertEquals(1, repository.profileUpdateCalls.size)
    }

    @Test
    fun `success signal stays until consumed and consuming clears it once`() {
        enqueueUpdate().complete(Result.success(PROFILE))
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())

        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01012345678"))

        assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)

        viewModel.onIntent(ProfileEditIntent.ConsumeEvent(ProfileEditEvent.UpdateFailure))

        assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)

        viewModel.onIntent(ProfileEditIntent.ConsumeEvent(ProfileEditEvent.UpdateSuccess))

        assertNull(viewModel.success().pendingEvent)
        assertTrue(viewModel.success().isUpdating)
    }

    @Test
    fun `failure allows retry and a late consume does not clear the newer signal`() {
        enqueueUpdate().complete(Result.failure(IllegalStateException("update failed")))
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())

        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = ""))

        assertEquals(ProfileEditEvent.UpdateFailure, viewModel.success().pendingEvent)
        assertFalse(viewModel.success().isUpdating)

        enqueueUpdate().complete(Result.success(PROFILE))
        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = ""))
        viewModel.onIntent(ProfileEditIntent.ConsumeEvent(ProfileEditEvent.UpdateFailure))

        assertEquals(2, repository.profileUpdateCalls.size)
        assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
    }

    @Test
    fun `cancelled update is not reported as failure`() {
        enqueueUpdate().completeExceptionally(CancellationException("cancelled"))
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())

        viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = ""))

        assertNull(viewModel.success().pendingEvent)
    }

    private fun enqueueUpdate(): CompletableDeferred<Result<User>> = CompletableDeferred<Result<User>>().also(updateGates::addLast)

    private fun ProfileEditViewModel.success(): ProfileEditUiState.Success = uiState.value as ProfileEditUiState.Success

    private companion object {
        val PROFILE = User("홍길동", "hong@afternote.local", null, null)
    }
}
