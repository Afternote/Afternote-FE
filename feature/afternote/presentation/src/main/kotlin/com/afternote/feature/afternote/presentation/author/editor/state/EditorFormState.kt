package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

internal const val CUSTOM_ADD_OPTION = "직접 추가하기"

/** 남기실 말씀이 없을 때 사용하는 초기 입력 항목. */
internal val DEFAULT_EDITOR_MESSAGE_BLOCKS: List<EditorMessageTextBlock> =
    listOf(EditorMessageTextBlock(title = "", body = ""))

/**
 * 저장과 프로세스 복원 대상인 에디터 폼 상태.
 *
 * 공통 입력은 직접 보유하고 종류별 입력은 [typeForm]으로 분리한다.
 * 다이얼로그·드롭다운·텍스트 필드 같은 Compose 상태는 [AfternoteEditorState]가 관리한다.
 */
data class EditorFormState(
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    /** [AfternoteEditorState.editorMessages]와 동기화되는 남기실 말씀 스냅샷. */
    val leaveMessageBlocks: List<EditorMessageTextBlock> = DEFAULT_EDITOR_MESSAGE_BLOCKS,
    /**
     * 복원된 [leaveMessageBlocks]를 Compose 입력 상태에 다시 적용하기 위한 세대 값.
     * 일반 입력 동기화에서는 변경하지 않는다.
     */
    val leaveMessageBlocksRestoreGeneration: Long = 0L,
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
                AfternoteType.SOCIAL_NETWORK -> AfternoteServiceCatalog.socialServices + CUSTOM_ADD_OPTION

                AfternoteType.GALLERY_AND_FILES -> AfternoteServiceCatalog.galleryServices

                // 비즈니스는 서비스 직접 추가를 지원하지 않는다.
                AfternoteType.BUSINESS -> AfternoteServiceCatalog.businessServices

                // 서비스 선택 UI가 없는 유형이다.
                AfternoteType.MEMORIAL, AfternoteType.ESTATE -> emptyList()
            }

    fun isCustomAddOption(service: String): Boolean = service == CUSTOM_ADD_OPTION
}
