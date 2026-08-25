package com.afternote.feature.afternote.presentation.author.editor.memorial

import androidx.compose.runtime.Stable
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverSection
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover

/**
 * 추억 노트 콘텐츠 파라미터
 */
@Stable
data class MemorialEditorContentParams(
    val displayMemorialPhotoUri: String?,
    val playlistAlbumCovers: List<AlbumCover>,
    val memorialVideoUrl: String?,
    val memorialThumbnailUrl: String? = null,
    val recipientSection: AfternoteEditorReceiverSection? = null,
    val onSongAddClick: () -> Unit,
    val onPhotoAddClick: () -> Unit,
    val onVideoAddClick: () -> Unit,
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
    val onThumbnailExtractionFailed: (Throwable) -> Unit = {},
)
