package com.afternote.afternote_fe.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [HomeTabScreen] 의 시각 회귀 baseline — Loading + Success 두 케이스.
 *
 * 의도된 시각 변경 시 `./gradlew :app:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeTabScreenLoadingScreenshot() {
    AfternoteTheme {
        HomeTabScreen(
            uiState = HomeTabUiState.Loading(cachedUserName = "일혁"),
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeTabScreenSuccessScreenshot() {
    AfternoteTheme {
        HomeTabScreen(
            uiState =
                HomeTabUiState.Success(
                    userName = "일혁",
                    isRecipientDesignated = false,
                    categoryCounts =
                        mapOf(
                            MindRecordCategory.DAILY_QUESTION to 10,
                            MindRecordCategory.DIARY to 8,
                            MindRecordCategory.DEEP_THOUGHT to 6,
                        ),
                ),
        )
    }
}
