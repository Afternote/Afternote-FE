package com.afternote.feature.receiver.presentation.playlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import com.afternote.feature.receiver.presentation.COMPACT_DEVICE_SPEC
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialPlaylistScreenScreenshot() {
    AfternoteTheme {
        MemorialPlaylistScreen(
            songs =
                listOf(
                    PlaylistSongDisplay(id = "1", title = "노래 1", artist = "아티스트 1"),
                    PlaylistSongDisplay(id = "2", title = "노래 2", artist = "아티스트 2"),
                ),
            onBackClick = {},
        )
    }
}

/**
 * 좁은 화면(360×800dp @320dpi) 변형 — 곡 제목·아티스트 행이 폭에 맞춰 줄어드는지 본다.
 *
 * 기준값은 [COMPACT_DEVICE_SPEC].
 */
@PreviewTest
@Preview(showBackground = true, device = COMPACT_DEVICE_SPEC)
@Composable
internal fun memorialPlaylistScreenCompactScreenshot() {
    AfternoteTheme {
        MemorialPlaylistScreen(
            songs =
                listOf(
                    PlaylistSongDisplay(id = "1", title = "노래 1", artist = "아티스트 1"),
                    PlaylistSongDisplay(id = "2", title = "노래 2", artist = "아티스트 2"),
                ),
            onBackClick = {},
        )
    }
}
