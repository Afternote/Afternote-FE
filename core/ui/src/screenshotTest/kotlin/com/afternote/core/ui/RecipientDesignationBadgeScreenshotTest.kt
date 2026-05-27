package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.badge.RecipientDesignationBadge
import com.afternote.core.ui.badge.RecipientDesignationBadgeState
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [RecipientDesignationBadge] 의 `Completed` / `Incomplete` 두 상태 baseline.
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun recipientDesignationBadgeCompletedScreenshot() {
    AfternoteTheme {
        RecipientDesignationBadge(state = RecipientDesignationBadgeState.Completed)
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun recipientDesignationBadgeIncompleteScreenshot() {
    AfternoteTheme {
        RecipientDesignationBadge(state = RecipientDesignationBadgeState.Incomplete(onClick = {}))
    }
}
