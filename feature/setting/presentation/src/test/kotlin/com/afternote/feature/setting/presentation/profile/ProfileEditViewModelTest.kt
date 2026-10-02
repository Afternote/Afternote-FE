package com.afternote.feature.setting.presentation.profile

import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakeMyProfileRepository.ProfileUpdateCall
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * 프로필 사진 변경의 저장 계약 (#1438).
 *
 * 고른 사진은 `profiles` 로 올린 뒤 받은 파일 키로 `PATCH users/me` 를 보낸다. 업로드가 실패하면
 * 수정 요청을 보내지 않고, 선택하지 않았으면 사진 칸을 널로 보내 서버 사진을 그대로 둔다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `잘못된 연락처는 사진 업로드와 프로필 수정 전에 차단한다`() =
        runTest(dispatcher) {
            for (phone in listOf("01055556666ggyyy", "0101234567", "02012345678", "010--1234-5678")) {
                val profileRepository = FakeMyProfileRepository(profile = SAVED.copy(phone = phone))
                val uploadRepository = FakePhotoUploadRepository.strict()
                val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
                runCurrent()
                viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))

                viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = phone))
                runCurrent()

                assertTrue(profileRepository.profileUpdateCalls.isEmpty())
                assertTrue(uploadRepository.uploads.isEmpty())
                assertFalse(viewModel.success().isUpdating)
                assertNull(viewModel.success().pendingEvent)
                assertEquals(PICKED_PHOTO, viewModel.success().selectedImageUri)
            }
        }

    @Test
    fun `빈 연락처와 유효한 휴대폰의 기존 저장 값을 보존한다`() =
        runTest(dispatcher) {
            for (phone in listOf("", "   ", "01012345678", "010-1234-5678", "011-123-4567")) {
                val profileRepository = FakeMyProfileRepository(profile = SAVED)
                val viewModel = ProfileEditViewModel(profileRepository, FakePhotoUploadRepository.strict())
                runCurrent()

                viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = phone))
                runCurrent()

                assertEquals(
                    listOf(ProfileUpdateCall(name = "새 이름", phone = phone.takeIf { it.isNotBlank() }, profileImageUrl = null)),
                    profileRepository.profileUpdateCalls,
                )
                assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
            }
        }

    @Test
    fun `잘못된 연락처를 고치면 보관한 사진과 함께 저장할 수 있다`() =
        runTest(dispatcher) {
            val profileRepository = FakeMyProfileRepository(profile = SAVED.copy(phone = "01055556666ggyyy"))
            val uploadRepository = FakePhotoUploadRepository(uploadedKey = UPLOADED_KEY)
            val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
            runCurrent()
            viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01055556666ggyyy"))
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01055556666"))
            runCurrent()

            assertEquals(listOf(PICKED_PHOTO to "profiles"), uploadRepository.uploads)
            assertEquals(
                listOf(ProfileUpdateCall(name = "새 이름", phone = "01055556666", profileImageUrl = UPLOADED_KEY)),
                profileRepository.profileUpdateCalls,
            )
            assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        }

    @Test
    fun `서버 프로필 사진을 싣고 고른 사진은 저장 전까지 그 위에만 보인다`() =
        runTest(dispatcher) {
            val viewModel = ProfileEditViewModel(FakeMyProfileRepository(profile = SAVED), FakePhotoUploadRepository.strict())
            runCurrent()

            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().displayImageUri)

            viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))

            assertEquals(PICKED_PHOTO, viewModel.success().selectedImageUri)
            assertEquals(PICKED_PHOTO, viewModel.success().displayImageUri)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
        }

    @Test
    fun `고른 사진은 profiles 로 올린 뒤 받은 파일 키로 수정 요청을 보낸다`() =
        runTest(dispatcher) {
            val order = mutableListOf<String>()
            val uploadRepository =
                FakePhotoUploadRepository(
                    onUpload = { _, _ ->
                        order += "upload"
                        Result.success(UploadedFile(fileUrl = UPLOADED_URL, fileKey = UPLOADED_KEY))
                    },
                )
            val profileRepository =
                FakeMyProfileRepository(profile = SAVED).apply {
                    onUpdateMyProfile = { _, _, _ ->
                        order += "patch"
                        SAVED
                    }
                }
            val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01011112222"))
            runCurrent()

            assertEquals(listOf("upload", "patch"), order)
            assertEquals(listOf(PICKED_PHOTO to "profiles"), uploadRepository.uploads)
            assertEquals(
                listOf(ProfileUpdateCall(name = "새 이름", phone = "01011112222", profileImageUrl = UPLOADED_KEY)),
                profileRepository.profileUpdateCalls,
            )
            assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        }

    @Test
    fun `사진을 고르지 않으면 업로드 없이 사진 칸을 비워 보내 기존 사진을 유지한다`() =
        runTest(dispatcher) {
            val profileRepository = FakeMyProfileRepository(profile = SAVED)
            val viewModel = ProfileEditViewModel(profileRepository, FakePhotoUploadRepository.strict())
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = ""))
            runCurrent()

            assertEquals(
                listOf(ProfileUpdateCall(name = "새 이름", phone = null, profileImageUrl = null)),
                profileRepository.profileUpdateCalls,
            )
            assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        }

    @Test
    fun `업로드가 실패하면 수정 요청을 보내지 않고 고른 사진을 그대로 둔다`() =
        runTest(dispatcher) {
            val uploadRepository = FakePhotoUploadRepository(onUpload = { _, _ -> Result.failure(IOException("upload failed")) })
            val profileRepository = FakeMyProfileRepository.strict().apply { onGetMyProfile = { SAVED } }
            val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01011112222"))
            runCurrent()

            assertEquals(1, uploadRepository.uploads.size)
            assertTrue(profileRepository.profileUpdateCalls.isEmpty())
            assertEquals(PICKED_PHOTO, viewModel.success().selectedImageUri)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertFalse(viewModel.success().isUpdating)
            assertEquals(ProfileEditEvent.UpdateFailure, viewModel.success().pendingEvent)
        }

    @Test
    fun `수정 요청이 실패해도 고른 사진은 남아 다시 저장할 수 있다`() =
        runTest(dispatcher) {
            val uploadRepository = FakePhotoUploadRepository(uploadedKey = UPLOADED_KEY)
            val profileRepository =
                FakeMyProfileRepository(profile = SAVED).apply {
                    onUpdateMyProfile = { _, _, _ -> throw IOException("offline") }
                }
            val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01011112222"))
            runCurrent()

            assertEquals(PICKED_PHOTO, viewModel.success().selectedImageUri)
            assertFalse(viewModel.success().isUpdating)
            assertEquals(ProfileEditEvent.UpdateFailure, viewModel.success().pendingEvent)
        }

    @Test
    fun `저장 중에 고른 사진은 받지 않아 올라가는 사진과 보이는 사진이 갈리지 않는다`() =
        runTest(dispatcher) {
            val pendingUpdate = CompletableDeferred<User>()
            val uploadRepository = FakePhotoUploadRepository(uploadedKey = UPLOADED_KEY)
            val profileRepository =
                FakeMyProfileRepository(profile = SAVED).apply {
                    onUpdateMyProfile = { _, _, _ -> pendingUpdate.await() }
                }
            val viewModel = ProfileEditViewModel(profileRepository, uploadRepository)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectPhoto(PICKED_PHOTO))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile(name = "새 이름", phone = "01011112222"))
            runCurrent()
            assertTrue(viewModel.success().isUpdating)

            viewModel.onIntent(ProfileEditIntent.SelectPhoto(OTHER_PHOTO))

            assertEquals(PICKED_PHOTO, viewModel.success().selectedImageUri)
            assertEquals(listOf(PICKED_PHOTO to "profiles"), uploadRepository.uploads)
            pendingUpdate.complete(SAVED)
            runCurrent()
        }

    private fun ProfileEditViewModel.success(): ProfileEditUiState.Success = uiState.value as ProfileEditUiState.Success

    private companion object {
        const val SERVER_IMAGE_URL = "https://cdn.test/profiles/saved.jpg?signature=abc"
        const val PICKED_PHOTO = "content://media/picker/0/profile/1"
        const val OTHER_PHOTO = "content://media/picker/0/profile/2"
        const val UPLOADED_URL = "https://cdn.test/profiles/new.jpg"
        const val UPLOADED_KEY = "profiles/tmp/new.jpg"
        val SAVED = User(name = "기존 이름", email = "user@afternote.local", phone = "01000000000", profileImageUrl = SERVER_IMAGE_URL)
    }
}
