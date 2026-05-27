package com.afternote.core.ui

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteTextField] 의 [TextFieldType] 4 variant baseline.
 *
 * 모든 케이스에서 `TextFieldState` 는 빈 상태 (`rememberTextFieldState()`) — placeholder/suffix 시각 회귀 가드 중심.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteTextFieldBasicScreenshot() {
    AfternoteTheme {
        AfternoteTextField(
            state = rememberTextFieldState(),
            placeholder = "이메일",
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteTextFieldSearchScreenshot() {
    AfternoteTheme {
        AfternoteTextField(
            state = rememberTextFieldState(),
            type = TextFieldType.Search,
            placeholder = "검색",
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteTextFieldVariant7Screenshot() {
    AfternoteTheme {
        AfternoteTextField(
            state = rememberTextFieldState(),
            type =
                TextFieldType.Variant7(
                    text = "인증번호 받기",
                    onClick = {},
                ),
            placeholder = "이메일",
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteTextFieldVariant8Screenshot() {
    AfternoteTheme {
        AfternoteTextField(
            state = rememberTextFieldState(),
            type = TextFieldType.Variant8(backState = rememberTextFieldState()),
            placeholder = "주민등록번호 앞 6자리",
        )
    }
}
