package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.setting.presentation.component.PinSetupStep
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun appLockSetupEnterNewScreenshot() {
    AppLockSetupScreenshotContent(step = PinSetupStep.ENTER_NEW, passwordLength = 0)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun appLockSetupConfirmNewScreenshot() {
    AppLockSetupScreenshotContent(step = PinSetupStep.CONFIRM_NEW, passwordLength = 3)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun appLockSetupEnterCurrentScreenshot() {
    AppLockSetupScreenshotContent(step = PinSetupStep.ENTER_CURRENT, passwordLength = 0)
}

@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun appLockSetupConfirmNewCompactScreenshot() {
    AppLockSetupScreenshotContent(step = PinSetupStep.CONFIRM_NEW, passwordLength = 3)
}

@Composable
private fun AppLockSetupScreenshotContent(
    step: PinSetupStep,
    passwordLength: Int,
) {
    AfternoteTheme {
        AppLockSetupContent(
            step = step,
            passwordLength = passwordLength,
            onDigitClick = {},
            onDeleteClick = {},
            onConfirmClick = {},
            onBack = {},
        )
    }
}
