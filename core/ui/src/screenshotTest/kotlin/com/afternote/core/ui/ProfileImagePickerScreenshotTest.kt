package com.afternote.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/**
 * [ProfileImagePicker] 의 시각 회귀 baseline — placeholder + 우하단 편집 배지(PlusBadgeButton).
 *
 * `displayImageUri = null` 케이스만 검증. 사진이 있을 때(`AsyncImage` 분기)는
 * ProfileImageLandscapePhotoScreenshotTest.kt 가 본다.
 *
 * 의도된 시각 변경 시 `./gradlew :core:ui:updateScreenshotTest` 로 baseline 갱신.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun profileImagePickerPlaceholderScreenshot() {
    AfternoteTheme {
        ProfileImagePicker(onPickClick = {})
    }
}
