package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
import kotlin.coroutines.cancellation.CancellationException

/**
 * 프로필 사진 변경의 조회·선택·전송 세 축 (#1438).
 *
 * 판정은 모두 공개 MVI 계약 — [ProfileEditIntent] 를 넣고 [ProfileEditUiState] 와 저장소에 남은
 * 호출을 본다. 업로드 경계는 `PhotoUploadRepository` fake 가 대신한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePhotoEditViewModelTest {
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
    fun `서버 프로필 사진을 그리고 저장 전 선택이 그것을 덮는다`() =
        runTest(dispatcher) {
            val viewModel = viewModel(profileRepository(user(profileImageUrl = SERVER_IMAGE_URL)))
            runCurrent()

            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().displayImageUri)

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))

            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)
            assertEquals(SELECTED_URI, viewModel.success().displayImageUri)
            // 저장 전까지 서버 정본은 그대로다 — 취소·실패가 기존 사진을 지우지 않는 근거.
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
        }

    @Test
    fun `고른 사진은 profiles 로 올리고 돌려받은 fileKey 를 수정 요청에 싣는다`() =
        runTest(dispatcher) {
            val repository = profileRepository(user(profileImageUrl = SERVER_IMAGE_URL))
            val photoUpload =
                FakePhotoUploadRepository(uploadedUrl = UPLOADED_URL, uploadedKey = UPLOADED_KEY)
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            assertEquals(listOf(SELECTED_URI to "profiles"), photoUpload.uploads.toList())
            assertEquals(
                listOf(FakeMyProfileRepository.ProfileUpdateCall("새 이름", "01012345678", UPLOADED_KEY)),
                repository.profileUpdateCalls.toList(),
            )
            assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        }

    @Test
    fun `사진을 고르지 않으면 업로드 없이 사진 필드를 널로 보낸다`() =
        runTest(dispatcher) {
            val repository = profileRepository(user(profileImageUrl = SERVER_IMAGE_URL))
            val photoUpload = FakePhotoUploadRepository.strict()
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            assertTrue(photoUpload.uploads.isEmpty())
            assertNull(repository.profileUpdateCalls.single().profileImageUrl)
        }

    @Test
    fun `재진입 갱신과 늦게 도착한 조회는 고른 사진을 지우지 않는다`() =
        runTest(dispatcher) {
            val pendingRead = CompletableDeferred<User>()
            var reads = 0
            val repository =
                FakeMyProfileRepository.strict().apply {
                    onGetMyProfile = {
                        if (++reads == 1) user(profileImageUrl = SERVER_IMAGE_URL) else pendingRead.await()
                    }
                }
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
            runCurrent()
            pendingRead.complete(user(name = "서버 이름", profileImageUrl = "https://cdn.test/other.jpg"))
            runCurrent()

            assertEquals("서버 이름", viewModel.success().name)
            assertEquals("https://cdn.test/other.jpg", viewModel.success().profileImageUrl)
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)
            assertEquals(SELECTED_URI, viewModel.success().displayImageUri)
        }

    @Test
    fun `업로드 실패는 수정 요청을 보내지 않고 고른 사진을 남겨 재시도할 수 있다`() =
        runTest(dispatcher) {
            val repository = profileRepository(user(profileImageUrl = SERVER_IMAGE_URL))
            val photoUpload =
                FakePhotoUploadRepository(
                    onUpload = { _, _ -> Result.failure(IllegalStateException("upload failed")) },
                )
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            assertTrue(repository.profileUpdateCalls.isEmpty())
            assertEquals(ProfileEditEvent.UpdateFailure, viewModel.success().pendingEvent)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)

            viewModel.onIntent(ProfileEditIntent.ConsumeEvent(ProfileEditEvent.UpdateFailure))
            photoUpload.onUpload = { _, _ -> Result.success(UploadedFile(UPLOADED_URL, UPLOADED_KEY)) }
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            assertEquals(UPLOADED_KEY, repository.profileUpdateCalls.single().profileImageUrl)
            assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        }

    @Test
    fun `수정 요청 실패도 기존 서버 사진과 고른 사진을 그대로 둔다`() =
        runTest(dispatcher) {
            val repository =
                profileRepository(user(profileImageUrl = SERVER_IMAGE_URL)).apply {
                    onUpdateMyProfile = { _, _, _ -> error("offline") }
                }
            val viewModel = viewModel(repository)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            assertEquals(ProfileEditEvent.UpdateFailure, viewModel.success().pendingEvent)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)
        }

    @Test
    fun `저장 중 중복 저장은 업로드를 한 번만 한다`() =
        runTest(dispatcher) {
            val pendingUpload = CompletableDeferred<Result<UploadedFile>>()
            val repository = profileRepository(user())
            val photoUpload = FakePhotoUploadRepository(onUpload = { _, _ -> pendingUpload.await() })
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            repeat(2) { viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678")) }
            runCurrent()

            assertEquals(1, photoUpload.uploads.size)
            assertTrue(repository.profileUpdateCalls.isEmpty())

            pendingUpload.complete(Result.success(UploadedFile(UPLOADED_URL, UPLOADED_KEY)))
            runCurrent()

            assertEquals(1, photoUpload.uploads.size)
            assertEquals(UPLOADED_KEY, repository.profileUpdateCalls.single().profileImageUrl)
        }

    @Test
    fun `저장 중 사진 선택은 받지 않는다`() =
        runTest(dispatcher) {
            val pendingUpload = CompletableDeferred<Result<UploadedFile>>()
            val repository = profileRepository(user())
            val photoUpload = FakePhotoUploadRepository(onUpload = { _, _ -> pendingUpload.await() })
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()
            viewModel.onIntent(ProfileEditIntent.SelectProfileImage("content://gallery/late"))

            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)

            pendingUpload.complete(Result.success(UploadedFile(UPLOADED_URL, UPLOADED_KEY)))
            runCurrent()

            assertEquals(listOf(SELECTED_URI to "profiles"), photoUpload.uploads.toList())
        }

    @Test
    fun `업로드 취소는 화면을 잠그지 않고 고른 사진을 남긴다`() =
        runTest(dispatcher) {
            val repository = profileRepository(user(profileImageUrl = SERVER_IMAGE_URL))
            val photoUpload =
                FakePhotoUploadRepository(onUpload = { _, _ -> throw CancellationException("cancelled") })
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            // 취소는 장애가 아니다 — 화면을 떠난 뒤 실패 스낵바나 뒤로가기가 일어나지 않도록 신호를 내지 않는다.
            assertTrue(repository.profileUpdateCalls.isEmpty())
            assertNull(viewModel.success().pendingEvent)
            // 그래도 진행 중 표시는 풀려야 한다. 참으로 굳으면 저장·선택·재진입이 전부 가드에 걸린다.
            assertFalse(viewModel.success().isUpdating)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)

            assertUnlockedAfterCancellation(viewModel, repository, photoUpload)
        }

    @Test
    fun `수정 요청 취소도 화면을 잠그지 않는다`() =
        runTest(dispatcher) {
            val repository =
                profileRepository(user(profileImageUrl = SERVER_IMAGE_URL)).apply {
                    onUpdateMyProfile = { _, _, _ -> throw CancellationException("cancelled") }
                }
            val photoUpload =
                FakePhotoUploadRepository(uploadedUrl = UPLOADED_URL, uploadedKey = UPLOADED_KEY)
            val viewModel = viewModel(repository, photoUpload)
            runCurrent()

            viewModel.onIntent(ProfileEditIntent.SelectProfileImage(SELECTED_URI))
            viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
            runCurrent()

            // 업로드까지는 나갔지만 수정 요청이 취소됐다 — 서버 사진은 승격 전이라 그대로다.
            assertEquals(1, photoUpload.uploads.size)
            assertNull(viewModel.success().pendingEvent)
            assertFalse(viewModel.success().isUpdating)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().profileImageUrl)
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)

            repository.onUpdateMyProfile = null
            assertUnlockedAfterCancellation(viewModel, repository, photoUpload)
        }

    /**
     * 취소 뒤에 화면이 다시 살아 있는지 — 사진 선택 · 재진입 갱신 · 저장 재시도 셋 다 통한다.
     *
     * 셋은 각각 다른 가드(`reduce` 의 `!isUpdating`, `loadProfile` 의 `isUpdating`,
     * `updateProfile` 의 `current.isUpdating`)를 지나므로, 하나만 보면 나머지가 잠긴 것을 놓친다.
     */
    private suspend fun TestScope.assertUnlockedAfterCancellation(
        viewModel: ProfileEditViewModel,
        repository: FakeMyProfileRepository,
        photoUpload: FakePhotoUploadRepository,
    ) {
        val readsBefore = repository.getProfileCalls
        viewModel.onIntent(ProfileEditIntent.SelectProfileImage(RETRY_URI))
        assertEquals(RETRY_URI, viewModel.success().selectedImageUri)

        viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
        viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
        runCurrent()
        assertEquals(readsBefore + 1, repository.getProfileCalls)

        photoUpload.onUpload = { _, _ -> Result.success(UploadedFile(UPLOADED_URL, UPLOADED_KEY)) }
        val updatesBefore = repository.profileUpdateCalls.size
        viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678"))
        runCurrent()

        assertEquals(updatesBefore + 1, repository.profileUpdateCalls.size)
        assertEquals(UPLOADED_KEY, repository.profileUpdateCalls.last().profileImageUrl)
        assertEquals(ProfileEditEvent.UpdateSuccess, viewModel.success().pendingEvent)
        assertFalse(viewModel.success().isUpdating)
    }

    private fun ProfileEditViewModel.success() = uiState.value as ProfileEditUiState.Success

    private fun viewModel(
        repository: FakeMyProfileRepository,
        photoUpload: FakePhotoUploadRepository = FakePhotoUploadRepository.strict(),
    ) = ProfileEditViewModel(repository, photoUpload)

    private fun profileRepository(user: User) =
        FakeMyProfileRepository.strict().apply {
            onGetMyProfile = { user }
            onUpdateMyProfile = { name, phone, profileImageUrl ->
                user.copy(
                    name = name ?: user.name,
                    phone = phone ?: user.phone,
                    profileImageUrl = profileImageUrl ?: user.profileImageUrl,
                )
            }
        }

    private fun user(
        name: String = "박서연",
        profileImageUrl: String? = null,
    ) = User(name, "test@afternote.com", "01012345678", profileImageUrl)

    private companion object {
        const val SERVER_IMAGE_URL = "https://cdn.test/profiles/saved.jpg"
        const val SELECTED_URI = "content://gallery/picked"
        const val RETRY_URI = "content://gallery/picked-again"
        const val UPLOADED_URL = "https://cdn.test/profiles/uploaded.jpg"
        const val UPLOADED_KEY = "profiles/tmp/uploaded.jpg"
    }
}
