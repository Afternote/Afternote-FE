package com.afternote.feature.afternote.presentation.author.edit.ui.content
import androidx.compose.runtime.Immutable
import com.afternote.core.ui.form.LastWishOption
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditReceiverSection
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover

/**
 * 추모 가이드라인 콘텐츠 파라미터
 */
@Immutable
data class MemorialGuidelineEditContentParams(
    val displayMemorialPhotoUri: String?,
    val playlistSongCount: Int,
    val playlistAlbumCovers: List<AlbumCover>,
    val selectedLastWish: String?,
    val lastWishOptions: List<LastWishOption>,
    val funeralVideoUrl: String?,
    val funeralThumbnailUrl: String? = null,
    val customLastWishText: String,
    val recipientSection: AfternoteEditReceiverSection? = null,
    val onSongAddClick: () -> Unit,
    val onLastWishSelected: (String) -> Unit,
    val onCustomLastWishChanged: (String) -> Unit,
    val onPhotoAddClick: () -> Unit,
    val onVideoAddClick: () -> Unit,
    val onThumbnailBytesReady: (ByteArray?) -> Unit = {},
)
