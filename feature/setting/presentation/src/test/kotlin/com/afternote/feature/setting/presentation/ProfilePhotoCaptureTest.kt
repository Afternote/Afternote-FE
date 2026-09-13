package com.afternote.feature.setting.presentation

import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.core.net.toUri
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * 프로필 사진 즉석 촬영 경로 (#1438) — 결과 파일 계약과 취소·실행 실패 처리.
 *
 * ### 왜 한 메서드에 다 들어 있나
 * `FileProvider` 는 authority 하나당 경로 전략을 **static** 으로 캐시하는데, Robolectric 은 테스트
 * *메서드마다* 다른 임시 dataDir 을 준다. 메서드를 나누면 두 번째 메서드부터 첫 메서드의 캐시(그
 * 메서드의 임시 경로)를 물려받아 「루트를 못 찾는다」로 실패한다. 실측으로 확인했고, 추억 노트의
 * `MemorialMediaCaptureFilesTest` 가 같은 이유로 같은 모양을 하고 있다. 프로덕션에는 없는 제약이라
 * 코드를 비트는 대신 테스트를 한 흐름으로 둔다 — 같은 이유로 갤러리 갈래는 [ProfilePhotoPickerTest] 에 있다.
 *
 * SDK 를 못 박는 이유: 라이브러리 모듈은 targetSdk 를 따로 두지 않아 compileSdk 가 그대로 실리는데
 * Robolectric 이 아는 최신 SDK 는 35 라, 두면 기동부터 실패한다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfilePhotoCaptureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun `촬영 결과는 좁힌 캐시 경로로만 노출되고 취소·실행 실패는 기존 사진을 남긴다`() {
        val registry = FakeMediaResultRegistry()
        val repository =
            FakeMyProfileRepository.strict().apply {
                onGetMyProfile = { User("박서연", "test@afternote.com", "01012345678", SERVER_IMAGE_URL) }
            }
        val viewModel = ProfileEditViewModel(repository, FakePhotoUploadRepository.strict())
        val activeViewModel = mutableStateOf(viewModel)
        val restorationTester = setContent(activeViewModel, registry)
        val captureDir = File(context.cacheDir, "profile_capture")

        // (1) 촬영 성공 — 카메라 앱이 써 넣은 그 파일이 선택으로 실린다.
        registry.captureSucceeds = true
        takePhoto()
        val captured = viewModel.success().selectedImageUri
        assertNotNull(captured)
        assertEquals(registry.lastCaptureTarget?.toString(), captured)
        assertEquals(captured, viewModel.success().displayImageUri)
        composeRule.onNodeWithContentDescription(PROFILE_IMAGE).assertIsDisplayed()

        // (2) 매니페스트 authority · setting_file_paths.xml 의 cache-path · 코드 상수가 서로 맞는가.
        //     어긋나면 IllegalArgumentException 인데 그 지점이 카메라를 띄우려는 순간이라 드러나는 시점이 늦다.
        val capturedUri = captured!!.toUri()
        assertEquals("content", capturedUri.scheme)
        assertEquals(context.packageName + ".setting.fileprovider", capturedUri.authority)
        // 업로드는 ContentResolver.getType() 으로 presigned 확장자를 정한다. octet-stream 이면 기본값으로 떨어진다.
        assertEquals("image/jpeg", context.contentResolver.getType(capturedUri))
        assertTrue(capturedUri.path.orEmpty().startsWith("/profile_capture/"))
        assertEquals(listOf("jpg"), captureDir.listFiles().orEmpty().map { it.extension })

        // (2-1) 노출 범위가 촬영 폴더로 좁혀져 있는가. cache-path 를 캐시 루트로 열어 두면 이 provider 로
        //       앱 캐시의 아무 파일이나 건네줄 수 있게 된다 — 그때 이 단언이 깨진다.
        val otherCacheFile = File(context.cacheDir, "unrelated.bin").apply { createNewFile() }
        assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, context.packageName + ".setting.fileprovider", otherCacheFile)
        }

        // (3) 촬영 취소(result=false) — 고른 사진은 그대로고, 우리가 만든 빈 파일만 사라진다.
        registry.captureSucceeds = false
        takePhoto()
        assertEquals(captured, viewModel.success().selectedImageUri)
        assertEquals(2, registry.captureLaunches)
        assertEquals(1, captureDir.listFiles().orEmpty().size)

        // (4) 촬영을 받아 줄 앱이 없을 때 — 안내가 뜨고, 사진도 남은 파일도 늘지 않는다.
        registry.captureAvailable = false
        takePhoto()
        assertEquals(captured, viewModel.success().selectedImageUri)
        assertEquals(3, registry.captureLaunches)
        assertEquals(1, captureDir.listFiles().orEmpty().size)
        composeRule.waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(CAPTURE_UNAVAILABLE).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(CAPTURE_UNAVAILABLE).assertIsDisplayed()

        // 촬영 중 프로세스가 재생성되어 새 ViewModel의 프로필 조회보다 결과가 먼저 돌아온다.
        registry.captureAvailable = true
        registry.captureSucceeds = true
        registry.deferCaptureResult = true
        takePhoto()
        val restoredCapture = checkNotNull(registry.lastCaptureTarget).toString()
        val pendingProfile = CompletableDeferred<User>()
        val restoredRepository =
            FakeMyProfileRepository.strict().apply { onGetMyProfile = { pendingProfile.await() } }
        val restoredViewModel = ProfileEditViewModel(restoredRepository, FakePhotoUploadRepository.strict())
        assertNotSame(viewModel, restoredViewModel)
        composeRule.runOnIdle { activeViewModel.value = restoredViewModel }
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.runOnIdle {
            assertEquals(ProfileEditUiState.Loading, restoredViewModel.uiState.value)
            registry.deliverCaptureResult()
        }

        // 결과가 도착했어도 GET이 끝나지 않은 구간을 한 번 더 복원한다.
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.runOnIdle {
            pendingProfile.complete(User("박서연", "test@afternote.com", "01012345678", SERVER_IMAGE_URL))
        }
        composeRule.waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) {
            restoredViewModel.uiState.value is ProfileEditUiState.Success
        }
        composeRule.runOnIdle {
            assertEquals(restoredCapture, restoredViewModel.success().selectedImageUri)
            assertEquals(SERVER_IMAGE_URL, restoredViewModel.success().profileImageUrl)
        }

        // 이미 전달한 결과가 재구성·복원 때 다시 적용되어 이후 선택을 덮어서는 안 된다.
        val subsequentChoice = "content://gallery/after-restoration"
        composeRule.runOnIdle {
            restoredViewModel.onIntent(ProfileEditIntent.SelectProfileImage(subsequentChoice))
        }
        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.runOnIdle {
            assertEquals(subsequentChoice, restoredViewModel.success().selectedImageUri)
        }
    }

    private fun takePhoto() {
        composeRule.openPhotoSourceSheet()
        composeRule.onNodeWithText(CAMERA_ITEM).performClick()
        composeRule.awaitPhotoSourceSheetClosed()
    }

    private fun setContent(
        viewModel: State<ProfileEditViewModel>,
        registry: FakeMediaResultRegistry,
    ): StateRestorationTester {
        val registryOwner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            AfternoteTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                    ProfileEditScreen(onBackClick = {}, onWithdrawGuideClick = {}, viewModel = viewModel.value)
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = PROFILE_TEST_TIMEOUT_MILLIS) {
            viewModel.value.uiState.value is ProfileEditUiState.Success
        }
        return restorationTester
    }
}
