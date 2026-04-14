package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.foundation.text.input.TextFieldState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.shared.LastWishOption
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover

/**
 * 추모 가이드라인 콘텐츠 파라미터
 */
data class MemorialGuidelineEditorContentParams(
    val displayMemorialPhotoUri: String?,
    val playlistSongCount: Int,
    val playlistAlbumCovers: List<AlbumCover>,
    val selectedLastWish: String?,
    val lastWishOptions: List<LastWishOption>,
    val funeralVideoUrl: String?,
    val funeralThumbnailUrl: String? = null,
    /** 기타(직접 입력) 필드 — [com.afternote.feature.afternote.presentation.shared.LastWishesRadioGroup]에 호이스팅 */
    val customLastWishState: TextFieldState,
    val recipientSection: AfternoteEditorReceiverSection? = null,
    val onSongAddClick: () -> Unit,
    val onLastWishSelected: (String) -> Unit,
    val onPhotoAddClick: () -> Unit,
    val onVideoAddClick: () -> Unit,
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)
