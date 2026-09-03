package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * 오류 안내 팝업의 시각 회귀 baseline — 네트워크 연결 오류 구성
 * (시안 `3628:23816`: 아이콘 원 + 제목 + 본문 + 단일 버튼).
 *
 * 카드 본체는 `ErrorPopup.kt` 파일 안에만 사는 구현이라, baseline 은 공개 진입점인
 * [NetworkErrorPopup] 을 그려서 잡는다 (#1672).
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun errorPopupNetworkScreenshot() {
    AfternoteTheme {
        NetworkErrorPopup(
            onRetry = {},
            onDismiss = {},
        )
    }
}
