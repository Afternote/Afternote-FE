package com.afternote.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.popup.AfternoteErrorPopupContent
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * 오류 안내 팝업(`AfternoteErrorPopup`)의 시각 회귀 baseline — 네트워크 연결 오류 구성
 * (시안 `3628:23816`: 아이콘 원 + 제목 + 본문 + 단일 버튼).
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun errorPopupNetworkScreenshot() {
    AfternoteTheme {
        AfternoteErrorPopupContent(
            iconRes = R.drawable.core_ui_ic_wifi_off,
            title = "네트워크 연결 오류",
            description = "인터넷 연결을 확인한 후 다시 시도해 주세요.",
            buttonText = "다시 시도하기",
            onButtonClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
