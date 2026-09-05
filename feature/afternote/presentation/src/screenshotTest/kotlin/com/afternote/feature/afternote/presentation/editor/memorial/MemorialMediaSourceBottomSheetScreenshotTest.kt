package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/** 영정 사진 슬롯 — 갤러리 / 사진 촬영. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialMediaSourceBottomSheetPhotoScreenshot() {
    AfternoteTheme {
        MemorialMediaSourceBottomSheet(
            target = MemorialMediaTarget.PHOTO,
            onPickFromGallery = {},
            onCapture = {},
            onRemove = null,
        )
    }
}

/** 장례식에 남길 영상 슬롯 — 아이콘과 문구만 갈린다. 두 벌을 두어 슬롯별 문구가 바뀌면 드러나게 한다. */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialMediaSourceBottomSheetVideoScreenshot() {
    AfternoteTheme {
        MemorialMediaSourceBottomSheet(
            target = MemorialMediaTarget.VIDEO,
            onPickFromGallery = {},
            onCapture = {},
            onRemove = null,
        )
    }
}

/** 첨부가 있는 슬롯 — 삭제 항목이 파괴적 동작 색(error)으로 더해진다 (#1114). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialMediaSourceBottomSheetRemovableScreenshot() {
    AfternoteTheme {
        MemorialMediaSourceBottomSheet(
            target = MemorialMediaTarget.VIDEO,
            onPickFromGallery = {},
            onCapture = {},
            onRemove = {},
        )
    }
}

/** 첨부가 있는 사진 슬롯 — 문구가 «사진 삭제» 로 갈린다. 서버 사진에도 이 항목이 열린다 (#1597). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialMediaSourceBottomSheetPhotoRemovableScreenshot() {
    AfternoteTheme {
        MemorialMediaSourceBottomSheet(
            target = MemorialMediaTarget.PHOTO,
            onPickFromGallery = {},
            onCapture = {},
            onRemove = {},
        )
    }
}
