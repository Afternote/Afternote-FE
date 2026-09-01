package com.afternote.feature.mindrecord.presentation.screen.memoryspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.mindrecord.presentation.R
import com.android.tools.screenshot.PreviewTest

/**
 * 좁은 화면 × 글자 확대 — 헤더가 화면 밖으로 뻗지 않는지 (#1153).
 *
 * 이 조합이 결함을 드러낸 조건이다. 360dp 기본 배율에서는 제목 블록의 내용 폭이 작아 우측
 * 여백이 남지만, 글자를 1.5배로 키우면 종전 헤더는 부제를 **화면 오른쪽 끝까지**(x=356.5dp /
 * 폭 360dp) 밀어냈다 — `start` 만 있고 `end` 패딩이 없었고, 제목 Column 에 weight 가 없어
 * 남은 폭을 상한으로 받지 못했기 때문이다.
 *
 * 크기 축(#1131)과 배율 축(#1146)이 **곱해져야** 드러나는 자리라, 둘 중 하나만으로는 잡히지
 * 않는다. 그래서 이 조합을 따로 고정한다.
 */
@PreviewTest
@Preview(
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
    device = COMPACT_DEVICE_SPEC,
    fontScale = LARGE_FONT_SCALE,
)
@Composable
internal fun memorySpaceScreenCompactLargeFontScreenshot() {
    AfternoteTheme {
        MemorySpaceContent(
            onBackClick = {},
            modifier = Modifier.fillMaxSize(),
            memories = emptyList(),
            statusText = stringResource(R.string.mindrecord_error_memory_space_failed),
            onRetryClick = {},
        )
    }
}

/**
 * Android 접근성 설정의 "크게" 구간. 이 값에서 #1153 이 드러났고 1.3 에서는 드러나지 않았다.
 */
internal const val LARGE_FONT_SCALE = 1.5f
