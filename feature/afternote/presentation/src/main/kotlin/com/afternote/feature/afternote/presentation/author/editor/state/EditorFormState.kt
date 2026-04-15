package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

internal const val CUSTOM_ADD_OPTION = "직접 추가하기"

internal val DEFAULT_EDITOR_MESSAGE_BLOCKS: List<EditorMessageTextBlock> =
    listOf(EditorMessageTextBlock(title = "", body = ""))

private const val LAST_WISH_DEFAULT_CALM = "차분하고 조용하게 보내주세요."
private const val LAST_WISH_DEFAULT_BRIGHT = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요."

/**
 * 에디터 **비즈니스/도메인** 폼 상태. [AfternoteEditorViewModel]의 [kotlinx.coroutines.flow.StateFlow]가 SSOT이며,
 * 프로세스 종료 대비 스냅샷은 [androidx.lifecycle.SavedStateHandle]에 JSON으로 저장한다.
 *
 * 순수 UI(다이얼로그·탭·드롭다운·[androidx.compose.foundation.text.input.TextFieldState])는
 * [AfternoteEditorUiState]가 담당한다.
 *
 * **남기실 말씀:** [messageBlocks]는 SavedState 스냅샷·Process Death 복원용 SSOT이며,
 * 화면의 [androidx.compose.foundation.text.input.TextFieldState]와 디바운스 동기화된다.
 *
 * **추모 플레이리스트:** [memorialPlaylistSongs]는 [com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder]와 동기화되어
 * [androidx.lifecycle.SavedStateHandle] JSON에 포함된다 (프로세스 종료·설정 변경 복원).
 *
 * **Bundle 용량:** 스냅샷이 들어가는 SavedState/번들은 대략 500KB~1MB를 넘기면 [android.os.TransactionTooLargeException] 위험이 있다.
 * 사진·썸네일은 Base64/data URL 같은 거대 문자열이 아니라 짧은 HTTPS URL 또는 content [android.net.Uri] 문자열만 두는 것이 안전하다.
 */
data class EditorFormState(
    val loadedItemId: String? = null,
    val selectedCategory: EditorCategory = EditorCategory.SOCIAL,
    val selectedService: String = AfternoteServiceCatalog.defaultSocialService,
    val selectedProcessingMethod: AccountProcessingMethod = AccountProcessingMethod.MEMORIAL_ACCOUNT,
    val selectedInformationProcessingMethod: InformationProcessingMethod =
        InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER,
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val socialProcessingMethods: List<ProcessingMethodItem> = emptyList(),
    val galleryProcessingMethods: List<ProcessingMethodItem> = emptyList(),
    val selectedLastWish: String? = null,
    val pickedMemorialPhotoUri: String? = null,
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val playlistSongCount: Int = 16,
    /** 추모(PLAYLIST) 곡 목록 — 홀더와 양방향 동기화 후 스냅샷에 저장. */
    val memorialPlaylistSongs: List<Song> = emptyList(),
    val playlistAlbumCovers: List<AlbumCover> = emptyList(),
    /** 저장·복원용 남기실 말씀 블록 (화면 TextField와 주기적으로 맞춘다). */
    val messageBlocks: List<EditorMessageTextBlock> = DEFAULT_EDITOR_MESSAGE_BLOCKS,
    /**
     * 0이 아니면 SavedState 등에서 폼이 복원된 뒤 UI에 블록을 한 번 밀어 넣어야 함을 뜻한다.
     * 타이핑 동기화(debounce)로 갱신할 때는 바꾸지 않는다.
     */
    val messageBlocksRestoreGeneration: Long = 0L,
) {
    /** Memorial(PLAYLIST) 저장용 atmosphere 문자열. 기타(직접 입력) 텍스트는 UI 레이어에서 넘긴다. */
    fun atmosphereForSave(customLastWishText: String): String =
        when (selectedLastWish) {
            "calm" -> LAST_WISH_DEFAULT_CALM
            "bright" -> LAST_WISH_DEFAULT_BRIGHT
            "other" -> customLastWishText.trim()
            else -> ""
        }

    fun displayMemorialPhotoUri(): String? = pickedMemorialPhotoUri ?: memorialPhotoUrl

    fun displayAlbumCovers(playlistStateHolder: MemorialPlaylistStateHolder?): List<AlbumCover> =
        when {
            playlistStateHolder?.songs?.isNotEmpty() == true -> {
                playlistStateHolder.songs.map { s ->
                    AlbumCover(id = s.id, imageUrl = s.albumCoverUrl, title = s.title)
                }
            }

            memorialPlaylistSongs.isNotEmpty() -> {
                memorialPlaylistSongs.map { s ->
                    AlbumCover(id = s.id, imageUrl = s.albumCoverUrl, title = s.title)
                }
            }

            else -> {
                playlistAlbumCovers
            }
        }

    fun livePlaylistSongCount(playlistStateHolder: MemorialPlaylistStateHolder?): Int =
        when {
            playlistStateHolder?.songs?.isNotEmpty() == true -> playlistStateHolder.songs.size
            memorialPlaylistSongs.isNotEmpty() -> memorialPlaylistSongs.size
            else -> playlistSongCount
        }

    val currentServiceOptions: List<String>
        get() =
            if (selectedCategory == EditorCategory.GALLERY) {
                AfternoteServiceCatalog.galleryServices
            } else {
                AfternoteServiceCatalog.socialServices + CUSTOM_ADD_OPTION
            }

    fun isCustomAddOption(service: String): Boolean = service == CUSTOM_ADD_OPTION
}
