package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.bottombar.BottomBar
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [BottomBar] 의 시각 회귀 baseline — HOME 탭 선택 상태.
 *
 * 모든 탭의 아이콘 + 라벨 + 선택 dot 인디케이터를 한 번에 가드. 다른 탭 선택 케이스는
 * 동일 레이아웃의 인디케이터 위치만 이동이라 본 baseline 1개로 충분.
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun bottomBarScreenshot() {
    AfternoteTheme {
        BottomBar(
            selectedNavTab = BottomNavTab.HOME,
            onTabClick = {},
        )
    }
}
