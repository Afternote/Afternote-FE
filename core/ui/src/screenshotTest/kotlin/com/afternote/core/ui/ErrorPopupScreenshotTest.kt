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

/**
 * 오류 안내 팝업(`AfternoteErrorPopup`)의 시각 회귀 baseline — 서버 오류 구성
 * (시안 `3628:23827`: 서버 스택 아이콘 원 + 제목 + 본문 + 단일 버튼).
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun errorPopupServerScreenshot() {
    AfternoteTheme {
        AfternoteErrorPopupContent(
            iconRes = R.drawable.core_ui_ic_server,
            title = "서버 오류",
            description = "서버에 문제가 발생했습니다.\n잠시 후 다시 시도해 주세요.",
            buttonText = "다시 시도하기",
            onButtonClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
