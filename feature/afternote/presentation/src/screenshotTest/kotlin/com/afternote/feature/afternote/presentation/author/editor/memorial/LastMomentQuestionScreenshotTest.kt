package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [LastMomentQuestion] 의 시각 회귀 baseline — 단순 라벨 (textField Medium + gray9).
 *
 * feature/afternote/presentation 모듈의 첫 baseline.
 * 의도된 시각 변경 시 `./gradlew :feature:afternote:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun lastMomentQuestionScreenshot() {
    AfternoteTheme {
        LastMomentQuestion(
            text = "가장 행복했던 순간을 떠올려 보세요.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
