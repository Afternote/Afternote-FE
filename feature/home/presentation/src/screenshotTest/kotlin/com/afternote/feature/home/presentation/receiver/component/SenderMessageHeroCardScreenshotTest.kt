package com.afternote.feature.home.presentation.receiver.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [SenderMessageHeroCard] 의 시각 회귀 baseline — 그라데이션 카드 + 송신자 메시지.
 *
 * `:app` 모듈의 첫 baseline. 1hyok 영역 "홈" 의 수신자 카드 시각 가드.
 * 의도된 시각 변경 시 `./gradlew :app:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun senderMessageHeroCardScreenshot() {
    AfternoteTheme {
        SenderMessageHeroCard(
            senderName = "서연",
            date = "2026.04.04",
            message = "내가 없어도 너의 시간이 멈추지 않고\n행복하게 흘러갔으면 좋겠어.",
            modifier = Modifier.padding(16.dp),
        )
    }
}
