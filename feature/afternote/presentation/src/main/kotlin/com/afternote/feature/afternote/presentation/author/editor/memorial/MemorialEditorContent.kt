package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.shared.MemorialContent
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist

/**
 * 추억 노트 종류 선택 시 표시되는 콘텐츠 (편집 모드).
 * [MemorialContent] 공통 레이아웃에 편집용 섹션 컴포저블을 넘깁니다.
 */
@Composable
fun MemorialEditorContent(
    modifier: Modifier = Modifier,
    params: MemorialEditorContentParams,
) {
    MemorialContent(
        introContent = { LastMomentQuestion() },
        photoContent = {
            MemorialPhotoUpload(
                displayImageUri = params.displayMemorialPhotoUri,
                onAddPhotoClick = params.onPhotoAddClick,
            )
        },
        playlistContent = {
            MemorialPlaylist(
                songCount = params.playlistAlbumCovers.size,
                albumCovers = params.playlistAlbumCovers,
                onCardClick = params.onSongAddClick,
            )
        },
        modifier = modifier,
        sectionSpacing = 32.dp,
        recipientContent = {
            params.recipientSection?.let { RecipientDesignationSection(section = it) }
        },
        videoContent = {
            MemorialVideoUpload(
                videoUrl = params.memorialVideoUrl,
                thumbnailUrl = params.memorialThumbnailUrl,
                onAddVideoClick = params.onVideoAddClick,
                onThumbnailBytesReady = params.onThumbnailBytesReady,
                onThumbnailExtractionFailed = params.onThumbnailExtractionFailed,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MemorialEditorContentPreview() {
    AfternoteTheme {
        MemorialEditorContent(
            params =
                MemorialEditorContentParams(
                    displayMemorialPhotoUri = null,
                    playlistAlbumCovers = emptyList(),
                    memorialVideoUrl = null,
                    onSongAddClick = {},
                    onPhotoAddClick = {},
                    onVideoAddClick = {},
                    onThumbnailBytesReady = {},
                ),
        )
    }
}
