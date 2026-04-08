package com.afternote.feature.afternote.presentation.author.editor.memorial
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.LastWishOption
import com.afternote.core.ui.LastWishOtherState
import com.afternote.core.ui.LastWishesRadioGroup
import com.afternote.core.ui.MemorialGuidelineContent
import com.afternote.core.ui.MemorialGuidelineSlots
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.receiver.RecipientDesignationSection
import com.afternote.feature.afternote.presentation.shared.detail.song.MemorialPlaylist

/**
 * 추모 가이드라인 종류 선택 시 표시되는 콘텐츠 (편집 모드).
 * [MemorialGuidelineContent] 공통 레이아웃을 사용하고, 슬롯에 편집용 컴포넌트를 채움.
 */
@Composable
fun MemorialGuidelineEditorContent(
    modifier: Modifier = Modifier,
    bottomPadding: PaddingValues,
    params: MemorialGuidelineEditorContentParams,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val bottomPaddingDp = bottomPadding.calculateBottomPadding()
    val viewportHeight =
        with(density) {
            windowInfo.containerSize.height.toDp() - bottomPaddingDp
        }
    val trailingSpacerHeight = viewportHeight * 0.1f

    MemorialGuidelineContent(
        modifier = modifier,
        slots =
            MemorialGuidelineSlots(
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
                                text = params.customLastWishText,
                                onTextChange = params.onCustomLastWishChanged,
                            ),
                    )
                },
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
            ),
        sectionSpacing = 32.dp,
        trailingSpacerHeight = trailingSpacerHeight,
    )
}

@Preview(showBackground = true)
@Composable
private fun MemorialGuidelineEditorContentPreview() {
    AfternoteTheme {
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
            bottomPadding = PaddingValues(bottom = 88.dp),
            params =
                MemorialGuidelineEditorContentParams(
                    displayMemorialPhotoUri = null,
                    playlistSongCount = 16,
                    playlistAlbumCovers = emptyList(),
                    selectedLastWish = "calm",
                    lastWishOptions = lastWishOptions,
                    funeralVideoUrl = null,
                    customLastWishText = "",
                    onSongAddClick = {},
                    onLastWishSelected = {},
                    onCustomLastWishChanged = {},
                    onPhotoAddClick = {},
                    onVideoAddClick = {},
                    onThumbnailBytesReady = {},
                ),
        )
    }
}
