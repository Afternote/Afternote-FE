package com.afternote.feature.setting.presentation.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeMyProfileRepository
import com.afternote.core.domain.testing.FakePhotoUploadRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR

/** 실제 Activity와 ActivityResultRegistry를 저장/복원한다. 피커 UI와 OS 프로세스 종료는 실행하지 않는다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProfileEditActivityRestorationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private var controller: ActivityController<ProfilePickerRestorationActivity>? = null

    @After
    fun tearDown() {
        controller?.pause()?.stop()?.destroy()
        controller = null
        ProfilePickerRestorationActivity.profileRepository = null
    }

    @Test
    fun `새 Activity에 복원된 피커 결과는 조회 완료까지 보관하고 두번째 복원 후에도 선택을 유지한다`() {
        val initialRepository = FakeMyProfileRepository(profile = SAVED)
        ProfilePickerRestorationActivity.profileRepository = initialRepository
        val first = launch()
        waitForSelection(first, null)
        val badge =
            ApplicationProvider
                .getApplicationContext<Context>()
                .getString(CoreUiR.string.core_ui_content_description_profile_edit)
        composeRule.onNodeWithContentDescription(badge).performClick()
        val request = checkNotNull(shadowOf(first).nextStartedActivityForResult)
        val firstState = destroyAndSave()

        // NonConfigurationInstances/VM을 넘기지 않고 새 Activity와 새 저장소를 만든다.
        val load = CompletableDeferred<User>()
        val restoredRepository = FakeMyProfileRepository.strict().apply { onGetMyProfile = { load.await() } }
        ProfilePickerRestorationActivity.profileRepository = restoredRepository
        val secondController = Robolectric.buildActivity(ProfilePickerRestorationActivity::class.java).create(firstState)
        controller = secondController
        val second = secondController.get()
        assertNotSame(first, second)
        assertNotSame(first.profileViewModel, second.profileViewModel)
        assertEquals(ProfileEditUiState.Loading, second.profileViewModel.uiState.value)

        // 아직 compose launcher가 재등록되기 전 raw ActivityResult를 실제 registry에 넣는다.
        assertTrue(
            second.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_OK, Intent().setData(Uri.parse(PICKED))),
        )
        secondController.start().resume().visible()
        composeRule.waitForIdle()
        assertEquals(ProfileEditUiState.Loading, second.profileViewModel.uiState.value)

        // 결과가 조회 대기 중 rememberSaveable에 들어간 뒤 다시 저장/복원한다.
        val secondState = destroyAndSave()
        val finalLoad = CompletableDeferred<User>()
        val finalRepository = FakeMyProfileRepository.strict().apply { onGetMyProfile = { finalLoad.await() } }
        ProfilePickerRestorationActivity.profileRepository = finalRepository
        val third = launch(secondState)
        composeRule.waitForIdle()
        assertNotSame(second.profileViewModel, third.profileViewModel)
        assertEquals(ProfileEditUiState.Loading, third.profileViewModel.uiState.value)
        finalLoad.complete(SAVED)
        waitForSelection(third, PICKED)

        composeRule.runOnIdle {
            assertEquals(PICKED, (third.profileViewModel.uiState.value as ProfileEditUiState.Success).displayImageUri)
            assertEquals(1, finalRepository.getProfileCalls)
            assertTrue(finalRepository.profileUpdateCalls.isEmpty())
            assertNull((first.profileViewModel.uiState.value as ProfileEditUiState.Success).selectedImageUri)
        }
    }

    private fun launch(state: Bundle? = null): ProfilePickerRestorationActivity {
        val next =
            Robolectric
                .buildActivity(ProfilePickerRestorationActivity::class.java)
                .create(state)
                .start()
                .resume()
                .visible()
        controller = next
        return next.get()
    }

    private fun waitForSelection(
        activity: ProfilePickerRestorationActivity,
        uri: String?,
    ) {
        composeRule.waitUntil(5_000) {
            val state = activity.profileViewModel.uiState.value
            state is ProfileEditUiState.Success && state.selectedImageUri == uri
        }
        composeRule.waitForIdle()
    }

    private fun destroyAndSave(): Bundle {
        val active = checkNotNull(controller)
        val state = Bundle()
        active
            .pause()
            .saveInstanceState(state)
            .stop()
            .destroy()
        controller = null
        // Parcel round trip은 원 Activity/registry의 메모리 객체 공유를 피한다.
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(state)
            parcel.setDataPosition(0)
            checkNotNull(parcel.readBundle(ProfilePickerRestorationActivity::class.java.classLoader))
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val PICKED = "content://media/picker/0/profile/1438"
        val SAVED = User(name = "QA", email = "qa@example.test", phone = null, profileImageUrl = null)
    }
}

/** 외부 서버와 DI만 대체한다. Activity의 registry와 saved-state 처리는 그대로 실행한다. */
class ProfilePickerRestorationActivity : ComponentActivity() {
    internal lateinit var profileViewModel: ProfileEditViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = checkNotNull(profileRepository)
        profileViewModel =
            ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        ProfileEditViewModel(repository, FakePhotoUploadRepository.strict()) as T
                },
            )[ProfileEditViewModel::class.java]
        setContent {
            AfternoteTheme {
                ProfileEditScreen(onBackClick = {}, onWithdrawGuideClick = {}, viewModel = profileViewModel)
            }
        }
    }

    companion object {
        internal var profileRepository: FakeMyProfileRepository? = null
    }
}
