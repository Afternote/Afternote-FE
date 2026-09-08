package com.afternote.feature.afternote.presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.afternote.presentation.LARGE_FONT_SCALE
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteCategoryRowAllScreenshot() {
    AfternoteTheme {
        AfternoteTypeFilterRow(
            onTabSelected = {},
            selectedTab = null,
        )
    }
}

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 탭 5개가 가용 폭에 들어가지 않아 더보기 화살표가 나타난다.
 *
 * 화살표가 마지막 탭 「추억 노트」 위에 겹쳐 글자를 가리던 #1141 의 회귀 가드다.
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun afternoteCategoryRowAllCompactScreenshot() {
    AfternoteTheme {
        AfternoteTypeFilterRow(
            onTabSelected = {},
            selectedTab = null,
        )
    }
}

/**
 * 글자 확대(×1.5) 변형 — 표준 화면(411×914dp)인데 글자만 커진 축.
 *
 * #1141 은 「좁은 화면 전용」이 아니라 가용 폭 대비 텍스트 폭 문제였다. 실측에서 이 조건의
 * 겹침비(1.0)가 좁은 화면(0.34)보다 컸으므로, 해상도 baseline 만으로는 재발을 못 잡는다.
 * 기준값은 [LARGE_FONT_SCALE].
 */
@PreviewTest
@Preview(showBackground = true, fontScale = LARGE_FONT_SCALE)
@Composable
internal fun afternoteCategoryRowAllLargeFontScreenshot() {
    AfternoteTheme {
        AfternoteTypeFilterRow(
            onTabSelected = {},
            selectedTab = null,
        )
    }
}
