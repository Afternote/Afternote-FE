package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.android.tools.screenshot.PreviewTest

/**
 * messageRes 가 실제로 반영되는지 보는 케이스라 **내부 폴백과 다른 문자열**을 쓴다.
 * 폴백(afternote_detail_load_error)과 같은 값을 넣으면 아래 폴백 케이스와 픽셀이 같아져
 * `messageRes ?:` 를 지워도 두 테스트가 통과해 버린다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailLoadErrorContentMessageResScreenshot() {
    AfternoteTheme {
        DetailLoadErrorContent(
            messageRes = R.string.afternote_detail_invalid_id,
            onBackClick = {},
        )
    }
}

/** messageRes 가 null 인 경로 — 컴포저블 내부 기본 폴백 문구가 그려지는지 확인한다. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailLoadErrorContentDefaultFallbackScreenshot() {
    AfternoteTheme {
        DetailLoadErrorContent(
            messageRes = null,
            onBackClick = {},
        )
    }
}

/**
 * onRetryClick 이 있는 경로 — 재시도 버튼이 문구 아래 그려지는지 확인한다.
 * 위 두 케이스는 `onRetryClick` 기본값(null)이라 버튼이 없는 상태를 고정한다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun detailLoadErrorContentRetryScreenshot() {
    AfternoteTheme {
        DetailLoadErrorContent(
            messageRes = null,
            onBackClick = {},
            onRetryClick = {},
        )
    }
}
