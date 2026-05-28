package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteCircularCheckbox] 의 Default(체크) + None(미체크) baseline.
 *
 * `Variant2` 분기 (비인터랙티브 체크 표시) 는 디자인 시안 추가 도착 시 별 case 로 확장.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteCircularCheckboxDefaultScreenshot() {
    AfternoteTheme {
        AfternoteCircularCheckbox(
            state = CheckboxState.Default,
            size = 24.dp,
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteCircularCheckboxNoneScreenshot() {
    AfternoteTheme {
        AfternoteCircularCheckbox(
            state = CheckboxState.None,
            size = 24.dp,
        )
    }
}
