package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.android.tools.screenshot.PreviewTest

/**
 * [AfternoteCategoryRow] 의 시각 회귀 baseline — `ALL` 탭 선택 상태.
 *
 * 다른 탭 선택은 인디케이터 위치만 이동 (시각 정합성 무관) 라 ALL 1 케이스로 가드.
 * 의도된 시각 변경 시 `./gradlew :feature:afternote:presentation:updateScreenshotTest` 로 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun afternoteCategoryRowAllScreenshot() {
    AfternoteTheme {
        AfternoteCategoryRow(
            onTabSelected = {},
            selectedTab = AfternoteCategory.ALL,
        )
    }
}
