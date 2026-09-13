package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.ui.testing.MinimumTouchTargetSize
import com.afternote.core.ui.testing.scanEnabledClickTargets
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.UpdateTimeLetterDeliveryConditionUseCase
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.navigation.SettingRoute
import com.afternote.feature.setting.presentation.viewmodel.DeliveryConditionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class DeliveryConditionTouchTargetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lastGreetingAction_acceptsTouchOutsideIts24dpVisualRow() {
        verifyLastGreetingTarget(fontScale = 1f, tapExpandedArea = true)
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp-xhdpi")
    fun lastGreetingAction_keeps48dpTargetAfterScrollingOnCompactScreenWithLargeText() {
        verifyLastGreetingTarget(fontScale = 2f, tapExpandedArea = false)
    }

    private fun verifyLastGreetingTarget(
        fontScale: Float,
        tapExpandedArea: Boolean,
    ) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val actionName = context.getString(R.string.last_greeting_edit_section_title)
        val repository = FakeUserReceiverRepository()
        val viewModel =
            DeliveryConditionViewModel(
                route = SettingRoute.AfterDeliveryRoute(receiverId = 1L),
                userRepository = repository,
                updateTimeLetterDeliveryCondition = UpdateTimeLetterDeliveryConditionUseCase(repository),
            )
        var actionCalls = 0
        var density = 0f
        composeRule.setContent {
            density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                AfternoteTheme {
                    DeliveryConditionScreen(
                        onBack = {},
                        onSaveSuccess = {},
                        onLastGreetingEditClick = { actionCalls++ },
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeRule.onNodeWithText(actionName).performScrollTo().assertIsDisplayed()
        val target = composeRule.scanEnabledClickTargets().single { it.name == actionName }
        val diagnostic =
            "fontScale=$fontScale touch=${target.width}x${target.height} " +
                "layout=${target.layoutWidth}x${target.layoutHeight}"
        println(diagnostic)
        assertFalse(diagnostic, target.isSmallerThan(MinimumTouchTargetSize))

        val tapY =
            if (tapExpandedArea) {
                assertEquals(24.dp.value, target.layoutHeight.value, 0.1f)
                assertTrue(diagnostic, target.touchBounds.top < target.layoutBounds.top)
                (target.touchBounds.top.value + target.layoutBounds.top.value) / 2f
            } else {
                assertTrue("Large text must actually enlarge the rendered row", target.layoutHeight > 24.dp)
                (target.touchBounds.top.value + target.touchBounds.bottom.value) / 2f
            }
        val tapX = (target.touchBounds.left.value + target.touchBounds.right.value) / 2f
        composeRule.onRoot().performTouchInput { click(Offset(tapX * density, tapY * density)) }
        composeRule.runOnIdle { assertEquals("The measured hit target must invoke the real screen action", 1, actionCalls) }
    }
}
