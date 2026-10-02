package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [MindRecordStatCard] 의 시각 회귀 baseline — 아이콘 + 라벨 + TOTAL + 카운트 통계 카드.
 *
 * 의도된 시각 변경 시 `./gradlew :app:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun mindRecordStatCardScreenshot() {
    AfternoteTheme {
        MindRecordStatCard(
            iconResId = com.afternote.core.ui.R.drawable.core_ui_ic_tabler_search,
            label = "데일리 질문",
            totalLabel = "TOTAL",
            count = 24,
            modifier = Modifier.padding(16.dp),
        )
    }
}
