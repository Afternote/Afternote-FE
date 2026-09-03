package com.afternote.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [HomeTabScreen] 의 시각 회귀 baseline — Loading + Success + Error 세 케이스.
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
            // 실제 오늘 날짜가 렌더되면 baseline 이 날마다 달라지므로 고정 날짜를 주입한다.
            todayDateText = "2026.04.10",
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
                    todayQuestionContent = "오늘 내가 배운\n가장 작은 교훈은 무엇인가요?",
                ),
            todayDateText = "2026.04.10",
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun homeTabScreenErrorScreenshot() {
    AfternoteTheme {
        HomeTabScreen(
            uiState = HomeTabUiState.Error(IllegalStateException("preview")),
            todayDateText = "2026.04.10",
        )
    }
}
