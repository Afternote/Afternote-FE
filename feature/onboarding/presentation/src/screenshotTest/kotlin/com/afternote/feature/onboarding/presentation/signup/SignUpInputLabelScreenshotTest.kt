package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SignUpInputLabel] 의 시각 회귀 baseline — 단순 라벨 텍스트 (bodyBase + gray9).
 *
 * feature 모듈 단위 screenshot test 적용 1차 시범. core/ui 가 아닌 feature 모듈의 첫 baseline.
 * 의도된 시각 변경 시 `./gradlew :feature:onboarding:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun signUpInputLabelScreenshot() {
    AfternoteTheme {
        SignUpInputLabel(
            text = "비밀번호 입력",
            modifier = Modifier.padding(16.dp),
        )
    }
}
