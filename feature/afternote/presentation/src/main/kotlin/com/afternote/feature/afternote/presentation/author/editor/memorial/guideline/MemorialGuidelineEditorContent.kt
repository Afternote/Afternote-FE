package com.afternote.feature.afternote.presentation.author.editor.memorial.guideline

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.memorial.FuneralVideoUpload
import com.afternote.feature.afternote.presentation.author.editor.memorial.LastMomentQuestion
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPhotoUpload
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.shared.LastWishOption
import com.afternote.feature.afternote.presentation.shared.LastWishOtherState
import com.afternote.feature.afternote.presentation.shared.LastWishesRadioGroup
import com.afternote.feature.afternote.presentation.shared.MemorialGuidelineContent
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist

/**
 * 추모 가이드라인 종류 선택 시 표시되는 콘텐츠 (편집 모드).
 * [MemorialGuidelineContent] 공통 레이아웃에 편집용 섹션 컴포저블을 넘깁니다.
 */
@Composable
fun MemorialGuidelineEditorContent(
    modifier: Modifier = Modifier,
    params: MemorialGuidelineEditorContentParams,
) {
    MemorialGuidelineContent(
        introContent = { LastMomentQuestion() },
        photoContent = {
            MemorialPhotoUpload(
                displayImageUri = params.displayMemorialPhotoUri,
                onAddPhotoClick = params.onPhotoAddClick,
            )
        },
        playlistContent = {
            MemorialPlaylist(
                songCount = params.playlistSongCount,
                albumCovers = params.playlistAlbumCovers,
                onAddSongClick = params.onSongAddClick,
            )
        },
        lastWishContent = {
            LastWishesRadioGroup(
                options = params.lastWishOptions,
                selectedValue = params.selectedLastWish,
                onOptionSelect = params.onLastWishSelected,
                otherState =
                    LastWishOtherState(
                        textFieldState = params.customLastWishState,
                    ),
            )
        },
        modifier = modifier,
        sectionSpacing = 32.dp,
        recipientContent = {
            params.recipientSection?.let { RecipientDesignationSection(section = it) }
        },
        videoContent = {
            FuneralVideoUpload(
                videoUrl = params.funeralVideoUrl,
                thumbnailUrl = params.funeralThumbnailUrl,
                onAddVideoClick = params.onVideoAddClick,
                onThumbnailBytesReady = params.onThumbnailBytesReady,
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun MemorialGuidelineEditorContentPreview() {
    AfternoteTheme {
        val customLastWishState = rememberTextFieldState()
        val lastWishOptions =
            listOf(
                LastWishOption(
                    text = "차분하고 조용하게 보내주세요.",
                    value = "calm",
                ),
                LastWishOption(
                    text = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요.",
                    value = "bright",
                ),
                LastWishOption(
                    text = "기타(직접 입력)",
                    value = "other",
                ),
            )

        MemorialGuidelineEditorContent(
            params =
                MemorialGuidelineEditorContentParams(
                    displayMemorialPhotoUri = null,
                    playlistSongCount = 16,
                    playlistAlbumCovers = emptyList(),
                    selectedLastWish = "calm",
                    lastWishOptions = lastWishOptions,
                    funeralVideoUrl = null,
                    customLastWishState = customLastWishState,
                    onSongAddClick = {},
                    onLastWishSelected = {},
                    onPhotoAddClick = {},
                    onVideoAddClick = {},
                    onThumbnailBytesReady = {},
                ),
        )
    }
}
