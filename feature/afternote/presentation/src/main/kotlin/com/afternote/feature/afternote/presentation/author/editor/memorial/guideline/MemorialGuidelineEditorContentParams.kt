package com.afternote.feature.afternote.presentation.author.editor.memorial.guideline

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.shared.LastWishOption

/**
 * 추모 가이드라인 콘텐츠 파라미터
 */
@Stable
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
