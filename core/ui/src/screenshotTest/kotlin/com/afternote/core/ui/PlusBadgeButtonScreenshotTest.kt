package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [PlusBadgeButton] 의 시각 회귀 baseline — 검정 원형 + plus 아이콘 (작은 인라인 배지).
 *
 * [ProfileImagePicker] 등에서 우하단 편집 배지로 합성되지만, 단독으로도 baseline 가드.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun plusBadgeButtonScreenshot() {
    AfternoteTheme {
        PlusBadgeButton(
            contentDescription = "추가",
            onClick = {},
        )
    }
}
