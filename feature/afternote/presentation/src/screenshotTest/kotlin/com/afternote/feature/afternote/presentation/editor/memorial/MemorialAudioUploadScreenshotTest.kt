package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

/** 미첨부 — 영상 슬롯과 같은 흰 카드 + 중앙 플러스 (#1118). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialAudioUploadEmptyScreenshot() {
    AfternoteTheme {
        MemorialAudioUpload(
            audioUrl = null,
            onAddAudioClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * 첨부 후 — 카드 안이 마이크 + 안내 문구로 바뀐다.
 *
 * 편집 화면 골든(`memorialEditorContentScreenshot`)은 프리뷰 프레임 높이에서 이 슬롯이 잘리므로,
 * 첨부 상태의 시각 회귀는 이 한 벌이 맡는다.
 */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialAudioUploadAttachedScreenshot() {
    AfternoteTheme {
        MemorialAudioUpload(
            audioUrl = "https://cdn.test/last-words.m4a",
            onAddAudioClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
