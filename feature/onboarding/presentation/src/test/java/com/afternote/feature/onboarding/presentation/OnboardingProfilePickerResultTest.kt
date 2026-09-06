package com.afternote.feature.onboarding.presentation

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.afternote.core.ui.R as CoreUiR

/**
 * 프로필 화면이 포토 피커 결과를 어떻게 다루는지 고정한다 (#1675).
 *
 * 결과 처리 helper 는 화면 파일 안에서만 쓰이는 `private` 구현이라 테스트가 직접 부르지 않는다.
 * 대신 화면의 공개 계약 — 「프로필 수정 버튼」을 눌러 피커를 띄우고 그 결과가 돌아왔을 때
 * `onProfileImagePick` 이 불리는가 — 으로 판정한다. 피커 자체는 실행하지 않고
 * [LocalActivityResultRegistryOwner] 를 결과를 즉시 돌려주는 레지스트리로 갈아 끼운다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingProfilePickerResultTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val pickedUris = mutableListOf<Uri>()

    /** 피커를 띄우면 [pickerResult] 를 그대로 돌려주는 화면을 세운다. */
    private fun setProfileContent(pickerResult: Uri?) {
        val registryOwner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = ImmediateResultRegistry(pickerResult)
            }
        composeRule.setContent {
            AfternoteTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides registryOwner) {
                    OnboardingProfileScreen(
                        initialName = "김노을",
                        displayImageUri = null,
                        snackbarHostState = remember { SnackbarHostState() },
                        onNameChange = {},
                        onProfileImagePick = pickedUris::add,
                        onBackClick = {},
                        onCompleteClick = {},
                    )
                }
            }
        }
    }

    private fun clickProfileImagePicker() {
        val editDescription =
            composeRule.activity.getString(CoreUiR.string.core_ui_content_description_profile_edit)
        composeRule.onNodeWithContentDescription(editDescription).performClick()
    }

    @Test
    fun `갤러리 취소는 기존 프로필 이미지를 변경하지 않는다`() {
        setProfileContent(pickerResult = null)

        clickProfileImagePicker()

        assertEquals(emptyList<Uri>(), pickedUris)
    }

    @Test
    fun `갤러리에서 고른 이미지는 프로필 이미지로 올라간다`() {
        val selectedUri = Uri.parse("content://profile/selected")
        setProfileContent(pickerResult = selectedUri)

        clickProfileImagePicker()

        assertEquals(listOf(selectedUri), pickedUris)
    }
}

/** `launch` 즉시 [result] 를 돌려주는 레지스트리. 실제 갤러리 Activity 를 띄우지 않는다. */
private class ImmediateResultRegistry(
    private val result: Uri?,
) : ActivityResultRegistry() {
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        @Suppress("UNCHECKED_CAST")
        dispatchResult(requestCode, result as O)
    }
}
