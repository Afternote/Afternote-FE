package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteButton] 의 시각 회귀 baseline — 가장 사용 빈도 높은 두 variant.
 *
 * main 의 `@Preview` 는 AS 미리보기 + 카탈로그 용도로 유지. 본 함수들은 baseline PNG 생성용.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteButtonDefaultScreenshot() {
    AfternoteTheme {
        AfternoteButton(
            text = "확인",
            onClick = {},
            type = AfternoteButtonType.Default,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteButtonLoadingScreenshot() {
    AfternoteTheme {
        AfternoteButton(
            text = "확인",
            onClick = {},
            type = AfternoteButtonType.Default,
            isLoading = true,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteButtonPlainScreenshot() {
    AfternoteTheme {
        AfternoteButton(
            text = "취소",
            onClick = {},
            type = AfternoteButtonType.Plain,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteButtonVariant5DualActionScreenshot() {
    AfternoteTheme {
        AfternoteButton(
            text = "전체 삭제",
            onClick = {},
            type = AfternoteButtonType.Variant5,
            secondaryText = "선택 삭제",
            onSecondaryClick = {},
        )
    }
}
