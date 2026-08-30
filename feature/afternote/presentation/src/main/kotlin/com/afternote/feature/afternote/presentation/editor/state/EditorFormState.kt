package com.afternote.feature.afternote.presentation.editor.state

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

/**
 * ViewModel이 소유하는 에디터 폼 상태.
 *
 * 공통 입력은 직접 보유하고 종류별 입력은 [typeForm]으로 분리한다.
 * 남기실 말씀과 Compose 전용 상태는 [AfternoteEditorState]가 관리한다.
 */
data class EditorFormState(
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val typeForm: AfternoteTypeForm = AfternoteTypeForm.Social(),
) {
    val selectedType: AfternoteType get() = typeForm.type

    /** `null`은 아직 서비스를 선택하지 않은 상태이며 저장 검증에서 거부된다. */
    val selectedService: String? get() = (typeForm as? AfternoteTypeForm.WithServiceAndProcessingMethods)?.selectedService

    val processingMethods: List<ProcessingMethodItem>
        get() = (typeForm as? AfternoteTypeForm.WithServiceAndProcessingMethods)?.processingMethods.orEmpty()

    val memorialForm: AfternoteTypeForm.Memorial? get() = typeForm as? AfternoteTypeForm.Memorial

    val pickedMemorialPhotoUri: String? get() = memorialForm?.pickedPhotoUri
    val memorialVideoUrl: String? get() = memorialForm?.videoUrl
    val memorialThumbnailUrl: String? get() = memorialForm?.thumbnailUrl
    val memorialPhotoUrl: String? get() = memorialForm?.photoUrl
    val memorialPlaylistSongs: List<Song> get() = memorialForm?.playlistSongs.orEmpty()

    fun displayMemorialPhotoUri(): String? = memorialForm?.displayPhotoUri()

    fun displayAlbumCovers(): List<AlbumCover> =
        memorialPlaylistSongs.map { s ->
            AlbumCover(imageUrl = s.albumCoverUrl, title = s.title)
        }

    val currentServiceOptions: List<String>
        get() =
            when (selectedType) {
                AfternoteType.SOCIAL_NETWORK -> AfternoteServiceCatalog.socialServices

                AfternoteType.GALLERY_AND_FILES -> AfternoteServiceCatalog.galleryServices

                AfternoteType.BUSINESS -> AfternoteServiceCatalog.businessServices

                // 서비스 선택 UI가 없는 유형이다.
                AfternoteType.MEMORIAL, AfternoteType.ESTATE -> emptyList()
            }
}
