package com.afternote.feature.setting.presentation

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.screen.ProfileEditScreen
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditIntent
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditUiState
import com.afternote.feature.setting.presentation.viewmodel.ProfileEditViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SELECTED_URI = "content://gallery/picked"

/**
 * 프로필 편집 화면의 편집 배지 → 소스 시트 → 갤러리 경로 (#1438).
 *
 * 피커 자체는 띄우지 않는다 — [LocalActivityResultRegistryOwner] 를 [FakeMediaResultRegistry] 로 갈아
 * 끼우고, 화면의 공개 계약(배지 탭 → 시트 → 상태·아바타)만 본다. 온보딩의
 * `OnboardingProfilePickerResultTest` 가 세운 관용구다.
 *
 * 촬영 갈래는 [ProfilePhotoCaptureTest] 가 따로 본다 — 그쪽 KDoc 에 적은 `FileProvider` 제약 때문에
 * 촬영 URI 를 만드는 테스트는 프로세스당 한 메서드여야 해서, 이 클래스는 촬영 항목을 누르지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfilePhotoPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `갤러리 취소는 기존 사진을 지우지 않는다`() {
        val registry = FakeMediaResultRegistry().apply { galleryResult = null }
        val viewModel = viewModel(user(profileImageUrl = SERVER_IMAGE_URL))
        setContent(viewModel, registry)

        composeRule.openPhotoSourceSheet()
        composeRule.onNodeWithText(GALLERY_ITEM).performClick()
        composeRule.awaitPhotoSourceSheetClosed()

        composeRule.runOnIdle {
            assertEquals(1, registry.galleryLaunches)
            assertNull(viewModel.success().selectedImageUri)
            assertEquals(SERVER_IMAGE_URL, viewModel.success().displayImageUri)
        }
        composeRule.onNodeWithContentDescription(PROFILE_IMAGE).assertIsDisplayed()
    }

    @Test
    fun `갤러리에서 고른 사진이 곧바로 아바타에 실린다`() {
        val registry = FakeMediaResultRegistry().apply { galleryResult = Uri.parse(SELECTED_URI) }
        val viewModel = viewModel(user(profileImageUrl = null))
        setContent(viewModel, registry)

        composeRule.runOnIdle { assertNull(viewModel.success().displayImageUri) }
        composeRule.onNodeWithContentDescription(PROFILE_IMAGE).assertIsDisplayed()

        composeRule.openPhotoSourceSheet()
        composeRule.onNodeWithText(GALLERY_ITEM).performClick()
        composeRule.awaitPhotoSourceSheetClosed()

        composeRule.runOnIdle {
            assertEquals(1, registry.galleryLaunches)
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)
            // 아바타가 그리는 값 자체를 본다 — 기본 아바타와 고른 사진은 같은 설명을 쓰므로
            // 노드 조회로는 갈리지 않는다.
            assertEquals(SELECTED_URI, viewModel.success().displayImageUri)
        }
        composeRule.onNodeWithContentDescription(PROFILE_IMAGE).assertIsDisplayed()
    }

    @Test
    fun `재진입 갱신은 고른 사진도 작성 중인 입력도 되돌리지 않는다`() {
        val registry = FakeMediaResultRegistry().apply { galleryResult = Uri.parse(SELECTED_URI) }
        val repository =
            FakeMyProfileRepository.strict().apply {
                onGetMyProfile = { user(profileImageUrl = null) }
            }
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
        setContent(viewModel, registry)
        composeRule.onNode(hasSetTextAction() and hasText("박서연")).performScrollTo().performTextReplacement("작성 중인 이름")

        composeRule.openPhotoSourceSheet()
        composeRule.onNodeWithText(GALLERY_ITEM).performClick()
        composeRule.awaitPhotoSourceSheetClosed()
        composeRule.runOnIdle {
            repository.onGetMyProfile = { User("서버 이름", "test@afternote.com", "01099998888", SERVER_IMAGE_URL) }
            viewModel.onIntent(ProfileEditIntent.RefreshOnReturn)
        }
        composeRule.waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) { viewModel.success().name == "서버 이름" }

        composeRule.runOnIdle {
            assertEquals(SELECTED_URI, viewModel.success().selectedImageUri)
            assertEquals(SELECTED_URI, viewModel.success().displayImageUri)
        }
        composeRule.onNodeWithText("작성 중인 이름").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `저장 중에는 배지가 그려지지 않아 시트도 피커도 열리지 않는다`() {
        val pendingUpdate = CompletableDeferred<User>()
        val registry = FakeMediaResultRegistry().apply { galleryResult = Uri.parse(SELECTED_URI) }
        val repository =
            FakeMyProfileRepository.strict().apply {
                onGetMyProfile = { user(profileImageUrl = null) }
                onUpdateMyProfile = { _, _, _ -> pendingUpdate.await() }
            }
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
        setContent(viewModel, registry)
        // 저장 전에는 눌린다 — 이 대조가 없으면 아래 단언이 「원래 없는 배지」도 통과시킨다.
        composeRule.onNodeWithContentDescription(ADD_BADGE).assertHasClickAction()

        composeRule.runOnIdle { viewModel.onIntent(ProfileEditIntent.UpdateProfile("새 이름", "01012345678")) }
        composeRule.waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) { viewModel.success().isUpdating }
        composeRule.waitForIdle()

        // 아바타는 남고 배지만 사라진다 — 누를 것이 없으니 시트를 여는 입구도 없다.
        composeRule.onNodeWithContentDescription(PROFILE_IMAGE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(ADD_BADGE).assertDoesNotExist()

        // 시트가 열리지 않았고, 어느 런처도 뜨지 않았고, 선택도 그대로다.
        composeRule.awaitPhotoSourceSheetClosed()
        composeRule.runOnIdle {
            assertEquals(0, registry.galleryLaunches)
            assertEquals(0, registry.captureLaunches)
            assertNull(viewModel.success().selectedImageUri)
        }
    }

    private fun setContent(
        viewModel: ProfileEditViewModel,
        registry: FakeMediaResultRegistry,
    ) {
        val registryOwner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        composeRule.setContent {
            AfternoteTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                    ProfileEditScreen(onBackClick = {}, onWithdrawGuideClick = {}, viewModel = viewModel)
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) {
            viewModel.uiState.value is ProfileEditUiState.Success
        }
    }

    private fun viewModel(user: User) =
        ProfileEditViewModel(
            FakeMyProfileRepository.strict().apply { onGetMyProfile = { user } },
            FakePhotoUploadRepository.strict(),
        )

    private fun user(profileImageUrl: String?) = User("박서연", "test@afternote.com", "01012345678", profileImageUrl)
}
