package com.afternote.feature.afternote.presentation.shared.fingerprint

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

/**
 * [FingerprintLoginScreen] 의 시각 회귀 baseline.
 *
 * `AfternoteNavGraph` 의 startDestination 이지만 앱 잠금이 켜지고 생체인증이 등록된 기기에서만
 * 실제로 노출된다 — 실기 QA 로는 지문을 등록해야 도달하므로 baseline 이 유일한 상시 감시축이다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun fingerprintLoginScreenScreenshot() {
    AfternoteTheme {
        FingerprintLoginScreen(onFingerprintAuthClick = {})
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
internal fun fingerprintLoginScreenCompactScreenshot() {
    AfternoteTheme {
        FingerprintLoginScreen(onFingerprintAuthClick = {})
    }
}
