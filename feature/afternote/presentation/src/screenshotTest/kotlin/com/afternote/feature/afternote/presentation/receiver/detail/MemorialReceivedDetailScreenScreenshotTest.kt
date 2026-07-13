package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.model.AlbumCover
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

private const val SAMPLE_LEAVE_MESSAGE =
    "이 계정에는 우리 가족 여행 사진이 많아. 계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!"

private val SAMPLE_ALBUM_COVERS =
    listOf(
        AlbumCover(id = "1"),
        AlbumCover(id = "2"),
        AlbumCover(id = "3"),
    )

/** 영상 없는 기본 상태 — 시안대로 영상 섹션이 숨겨진다 (#274). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialReceivedDetailScreenScreenshot() {
    AfternoteTheme {
        MemorialReceivedDetailScreen(
            senderName = "서연",
            leaveMessage = SAMPLE_LEAVE_MESSAGE,
            albumCovers = SAMPLE_ALBUM_COVERS,
            songCount = 16,
        )
    }
}

/** 영상이 있는 상태 — 조건부 영상 섹션이 노출된다 (#274). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialReceivedDetailScreenWithVideoScreenshot() {
    AfternoteTheme {
        MemorialReceivedDetailScreen(
            senderName = "서연",
            leaveMessage = SAMPLE_LEAVE_MESSAGE,
            albumCovers = SAMPLE_ALBUM_COVERS,
            songCount = 16,
            memorialVideoUrl = "https://example.com/memorial.mp4",
        )
    }
}
