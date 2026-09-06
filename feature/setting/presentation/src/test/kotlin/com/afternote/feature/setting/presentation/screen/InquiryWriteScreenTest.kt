package com.afternote.feature.setting.presentation.screen

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.app.ActivityOptionsCompat
import com.afternote.core.ui.theme.AfternoteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class InquiryWriteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingTooManyAttachments_showsLimitAndAllowsRemoval() {
        val registry = ScreenshotPickerRegistry((1..5).map { Uri.parse("content://inquiry/image/$it") })
        showForm(registry)

        composeRule.onNodeWithText("스크린샷 추가하기 (0/3장)").performScrollTo().performClick()

        composeRule.onNodeWithText("스크린샷 추가하기 (3/3장)").assertIsNotEnabled()
        composeRule.onNodeWithText("스크린샷은 최대 3장까지 첨부할 수 있어요.").assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule.onNodeWithContentDescription("스크린샷 1 삭제").performScrollTo().performClick()
        composeRule.onNodeWithText("스크린샷 추가하기 (2/3장)").assertIsEnabled()
    }

    @Test
    fun restoredForm_keepsAttachmentsAndText() {
        val registry = ScreenshotPickerRegistry(listOf(Uri.parse("content://inquiry/image/1")))
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry) {
                AfternoteTheme { InquiryWriteScreen(onBackClick = {}) }
            }
        }
        composeRule.onNodeWithText("제목을 입력해 주세요.").performTextInput("문의 제목")
        composeRule.onNodeWithText("내용을 입력해 주세요.").performScrollTo().performTextInput("문의 내용")
        composeRule.onNodeWithText("스크린샷 추가하기 (0/3장)").performScrollTo().performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("문의 제목").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("문의 내용").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("스크린샷 추가하기 (1/3장)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("스크린샷 1 삭제").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unsupportedSubmission_keepsWrittenFormAndDoesNotNavigateBack() {
        var backCalls = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides ScreenshotPickerRegistry(emptyList())) {
                AfternoteTheme { InquiryWriteScreen(onBackClick = { backCalls++ }) }
            }
        }
        composeRule.onNodeWithText("문의 접수하기").assertIsNotEnabled()
        composeRule.onNodeWithText("제목을 입력해 주세요.").performTextInput("문의 제목")
        composeRule.onNodeWithText("내용을 입력해 주세요.").performScrollTo().performTextInput("문의 내용")
        composeRule.onNodeWithText("문의 접수하기").performClick()

        composeRule.onNodeWithText("문의 접수 기능은 아직 준비 중이에요. 조금만 기다려 주세요.").assertIsDisplayed()
        composeRule.onNodeWithText("문의 제목").performScrollTo().assertIsDisplayed()
        assertEquals(0, backCalls)
    }

    private fun showForm(registry: ScreenshotPickerRegistry) {
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry) {
                AfternoteTheme { InquiryWriteScreen(onBackClick = {}) }
            }
        }
    }
}

private class ScreenshotPickerRegistry(
    private val selection: List<Uri>,
) : ActivityResultRegistry(),
    ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = this

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        dispatchResult(requestCode, selection)
    }
}
