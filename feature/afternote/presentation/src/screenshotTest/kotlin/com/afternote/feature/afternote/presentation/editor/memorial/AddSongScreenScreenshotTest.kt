package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.COMPACT_DEVICE_SPEC
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.android.tools.screenshot.PreviewTest

/**
 * [AddSongScreen] 의 시각 회귀 baseline — 검색 결과 목록.
 *
 * 실기 QA 로는 도달 비용이 큰 화면이다(에디터 → 추억 노트 → 플레이리스트 → 노래 추가).
 * 선택 상태는 `SelectableSongListBody` 내부 소유라 상태로 만들 수 없어 목록만 담는다.
 */
private val SAMPLE_SONGS =
    (1..9).map { i ->
        PlaylistSongDisplay(selectionKey = "s$i", title = "노래 제목 $i", artist = "가수 이름")
    }

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun addSongScreenScreenshot() {
    AfternoteTheme {
        AddSongScreen(
            uiState = AddSongUiState(songs = SAMPLE_SONGS),
            onSearchQueryChange = {},
            onErrorConsumed = {},
            onBackClick = {},
            onSongsAdded = {},
        )
    }
}

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 검색창·목록 행이 폭에 맞춰 줄어드는지 본다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun addSongScreenCompactScreenshot() {
    AfternoteTheme {
        AddSongScreen(
            uiState = AddSongUiState(songs = SAMPLE_SONGS),
            onSearchQueryChange = {},
            onErrorConsumed = {},
            onBackClick = {},
            onSongsAdded = {},
        )
    }
}
