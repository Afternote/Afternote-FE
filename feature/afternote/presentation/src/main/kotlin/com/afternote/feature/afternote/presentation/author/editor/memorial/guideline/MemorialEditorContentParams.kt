package com.afternote.feature.afternote.presentation.author.editor.memorial.guideline

import androidx.compose.runtime.Stable
import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection

/**
 * 추억 노트 콘텐츠 파라미터
 */
@Stable
data class MemorialEditorContentParams(
    val displayMemorialPhotoUri: String?,
    val playlistSongCount: Int,
    val playlistAlbumCovers: List<AlbumCover>,
    val funeralVideoUrl: String?,
    val funeralThumbnailUrl: String? = null,
    val recipientSection: AfternoteEditorReceiverSection? = null,
    val onSongAddClick: () -> Unit,
    val onPhotoAddClick: () -> Unit,
    val onVideoAddClick: () -> Unit,
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)
