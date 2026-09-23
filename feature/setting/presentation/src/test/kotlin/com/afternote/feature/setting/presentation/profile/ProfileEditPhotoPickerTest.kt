package com.afternote.feature.setting.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import com.afternote.core.ui.R as CoreUiR

/**
 * 프로필 편집 화면의 사진 배지 → 갤러리 선택 결과 처리 (#1438).
 *
 * 피커 자체는 띄우지 않는다. [LocalActivityResultRegistryOwner] 를 결과를 정해 둔 레지스트리로 갈아
 * 끼우고 화면의 공개 계약(배지 탭 → 피커 요청 → ViewModel 의 선택)만 본다. 온보딩의
 * `OnboardingProfilePickerResultTest` 와 같은 관용구다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileEditPhotoPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val pickBadge =
        ApplicationProvider
            .getApplicationContext<Context>()
            .getString(CoreUiR.string.core_ui_content_description_profile_edit)

    @Test
    fun `배지는 갤러리 이미지 선택만 요청하고 취소는 기존 사진을 지우지 않는다`() {
        val registry = GalleryResultRegistry(result = null)
        val viewModel = loadedViewModel()
        setContent(registry) { viewModel }
        awaitSuccess(viewModel)

        composeRule.onNodeWithContentDescription(pickBadge).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(PickVisualMedia.ImageOnly), registry.requestedMediaTypes)
            assertNull(viewModel.success().selectedImageUri)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().displayImageUri)
        }
    }

    @Test
    fun `갤러리에서 고른 사진은 저장 전 선택으로 아바타에 실린다`() {
        val registry = GalleryResultRegistry(result = Uri.parse(PICKED_PHOTO))
        val viewModel = loadedViewModel()
        setContent(registry) { viewModel }
        awaitSuccess(viewModel)

        composeRule.onNodeWithContentDescription(pickBadge).performClick()

        composeRule.waitUntil(TIMEOUT_MILLIS) { viewModel.success().selectedImageUri == PICKED_PHOTO }
        composeRule.runOnIdle { assertEquals(PICKED_PHOTO, viewModel.success().displayImageUri) }
    }

    @Test
    fun `프로필 조회가 끝나기 전에 돌아온 사진은 보관했다가 조회가 끝나면 넘긴다`() {
        // 갤러리에 다녀오는 사이 프로세스가 회수된 경우를 흉내 낸다. 화면은 그대로 복원되고 ViewModel 만
        // 새로 생겨 조회 중일 때 피커 결과가 먼저 도착한다.
        val registry = GalleryResultRegistry(result = null, deferResult = true)
        val firstViewModel = loadedViewModel()
        val pendingProfile = CompletableDeferred<User>()
        val restoredViewModel =
            ProfileEditViewModel(
                FakeMyProfileRepository.strict().apply { onGetMyProfile = { pendingProfile.await() } },
                FakePhotoUploadRepository.strict(),
            )
        var current by mutableStateOf(firstViewModel)
        setContent(registry) { current }
        awaitSuccess(firstViewModel)
        composeRule.onNodeWithContentDescription(pickBadge).performClick()

        composeRule.runOnIdle { current = restoredViewModel }
        composeRule.waitForIdle()
        composeRule.runOnIdle { registry.deliver(Uri.parse(PICKED_PHOTO)) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(ProfileEditUiState.Loading, restoredViewModel.uiState.value) }

        pendingProfile.complete(SAVED)

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            (restoredViewModel.uiState.value as? ProfileEditUiState.Success)?.selectedImageUri == PICKED_PHOTO
        }
        composeRule.runOnIdle { assertNull(firstViewModel.success().selectedImageUri) }
    }

    @Test
    fun `저장 중에 돌아온 사진은 보관했다가 저장이 실패한 뒤에 넘긴다`() {
        val registry = GalleryResultRegistry(result = null, deferResult = true)
        val pendingUpdate = CompletableDeferred<User>()
        val viewModel =
            ProfileEditViewModel(
                FakeMyProfileRepository(profile = SAVED).apply {
                    onUpdateMyProfile = { _, _, _ -> pendingUpdate.await() }
                },
                FakePhotoUploadRepository.strict(),
            )
        setContent(registry) { viewModel }
        awaitSuccess(viewModel)
        composeRule.onNodeWithContentDescription(pickBadge).performClick()

        composeRule.runOnIdle { viewModel.updateProfile(name = "새 이름", phone = "01011112222") }
        composeRule.waitUntil(TIMEOUT_MILLIS) { viewModel.success().isUpdating }
        composeRule.runOnIdle { registry.deliver(Uri.parse(PICKED_PHOTO)) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertNull(viewModel.success().selectedImageUri) }

        pendingUpdate.completeExceptionally(IOException("offline"))

        composeRule.waitUntil(TIMEOUT_MILLIS) { viewModel.success().selectedImageUri == PICKED_PHOTO }
    }

    private fun setContent(
        registry: GalleryResultRegistry,
        viewModel: () -> ProfileEditViewModel,
    ) {
        val registryOwner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        composeRule.setContent {
            AfternoteTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                    ProfileEditScreen(onBackClick = {}, onWithdrawGuideClick = {}, viewModel = viewModel())
                }
            }
        }
    }

    private fun awaitSuccess(viewModel: ProfileEditViewModel) {
        composeRule.waitUntil(TIMEOUT_MILLIS) { viewModel.uiState.value is ProfileEditUiState.Success }
        composeRule.waitForIdle()
    }

    private fun loadedViewModel() =
        ProfileEditViewModel(
            FakeMyProfileRepository.strict().apply { onGetMyProfile = { SAVED } },
            FakePhotoUploadRepository.strict(),
        )

    private fun ProfileEditViewModel.success(): ProfileEditUiState.Success = uiState.value as ProfileEditUiState.Success

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
        const val SERVER_IMAGE_URL = "https://cdn.test/profiles/saved.jpg"
        const val PICKED_PHOTO = "content://media/picker/0/profile/1"
        val SAVED = User(name = "기존 이름", email = "user@afternote.local", phone = "01000000000", profileImageUrl = SERVER_IMAGE_URL)
    }
}

/** 갤러리 피커 요청을 기록하고, 정해 둔 결과를 즉시 또는 [deliver] 때 돌려주는 레지스트리. */
private class GalleryResultRegistry(
    private val result: Uri?,
    private val deferResult: Boolean = false,
) : ActivityResultRegistry() {
    val requestedMediaTypes = mutableListOf<PickVisualMedia.VisualMediaType>()
    private var pendingRequestCode: Int? = null

    fun deliver(uri: Uri?) {
        val requestCode = checkNotNull(pendingRequestCode) { "피커 요청이 없다" }
        pendingRequestCode = null
        dispatchResult(requestCode, uri)
    }

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        requestedMediaTypes += (input as PickVisualMediaRequest).mediaType
        if (deferResult) {
            pendingRequestCode = requestCode
        } else {
            @Suppress("UNCHECKED_CAST")
            dispatchResult(requestCode, result as O)
        }
    }
}
