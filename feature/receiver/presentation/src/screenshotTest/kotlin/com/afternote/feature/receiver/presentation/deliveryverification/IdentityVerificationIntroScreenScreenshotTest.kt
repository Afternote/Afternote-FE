package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.receiver.presentation.LARGE_FONT_SCALE
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun identityVerificationIntroScreenScreenshot() {
    AfternoteTheme {
        IdentityVerificationIntroScreen(
            onBackClick = {},
            onStartClick = {},
        )
    }
}

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 스크롤이 없는 화면이라 세로가 모자라면 그대로 잘린다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun identityVerificationIntroScreenCompactScreenshot() {
    AfternoteTheme {
        IdentityVerificationIntroScreen(
            onBackClick = {},
            onStartClick = {},
        )
    }
}

/**
 * 글자 확대(×1.5) 변형 — 본인 확인 안내.
 *
 * 화면 크기와 다른 축이라 좁은 화면 baseline 으로는 잡히지 않는다. 기준값은 [LARGE_FONT_SCALE].
 */
@PreviewTest
@Preview(showBackground = true, fontScale = LARGE_FONT_SCALE)
@Composable
internal fun identityVerificationIntroScreenLargeFontScreenshot() {
    AfternoteTheme {
        IdentityVerificationIntroScreen(
            onBackClick = {},
            onStartClick = {},
        )
    }
}
