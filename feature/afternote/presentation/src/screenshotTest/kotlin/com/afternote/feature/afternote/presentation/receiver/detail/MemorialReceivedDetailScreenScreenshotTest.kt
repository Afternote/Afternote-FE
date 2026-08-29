package com.afternote.feature.afternote.presentation.receiver.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.MessageBlockUiModel
import com.android.tools.screenshot.PreviewTest

/** 시안이 블록 2개 상태를 규격으로 두므로, 제목 있는 블록과 없는 블록을 함께 담아 카드 반복까지 렌더에 담는다. */
private val SAMPLE_MESSAGE_BLOCKS =
    listOf(
        MessageBlockUiModel(
            title = "가족에게",
            body = "이 계정에는 우리 가족 여행 사진이 많아. 계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
        ),
        MessageBlockUiModel(body = "기일에는 이 노래들을 함께 들어 줘."),
    )

private val SAMPLE_ALBUM_COVERS =
    listOf(
        AlbumCover(),
        AlbumCover(),
        AlbumCover(),
    )

/** 영상 없는 기본 상태 — 시안대로 영상 섹션이 숨겨진다 (#274). */
@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun memorialReceivedDetailScreenScreenshot() {
    AfternoteTheme {
        MemorialReceivedDetailScreen(
            senderName = "서연",
            onNavigateToFullList = {},
            onNavigateToPlaylist = {},
            onBackClick = {},
            messageBlocks = SAMPLE_MESSAGE_BLOCKS,
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
            onNavigateToFullList = {},
            onNavigateToPlaylist = {},
            onBackClick = {},
            messageBlocks = SAMPLE_MESSAGE_BLOCKS,
            albumCovers = SAMPLE_ALBUM_COVERS,
            songCount = 16,
            memorialVideoUrl = "https://example.com/memorial.mp4",
        )
    }
}
