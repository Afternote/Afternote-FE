package com.afternote.core.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.popup.AfternotePopupCardLayout
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternotePopupCardLayout] 의 시각 회귀 baseline — 메시지 + 단일 버튼 시뮬레이션.
 *
 * `Popup` / `PopupContent` 가 본 layout 을 공유하므로 카드 자체의 외곽선·padding·drop shadow 가드.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternotePopupCardLayoutScreenshot() {
    AfternoteTheme {
        AfternotePopupCardLayout(
            modifier = Modifier.padding(16.dp),
            message = "삭제하시겠어요?",
        ) {
            Text(
                text = "확인",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
