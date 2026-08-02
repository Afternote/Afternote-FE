package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

internal const val CUSTOM_ADD_OPTION = "직접 추가하기"

internal val DEFAULT_EDITOR_MESSAGE_BLOCKS: List<EditorMessageTextBlock> =
    listOf(EditorMessageTextBlock(title = "", body = ""))

/**
 * 에디터 **비즈니스/도메인** 폼 상태.
 *
 * 필드를 개별 스트림으로 쪼개지 않고 하나의 불변 data class로 묶은 근거는 UI 레이어 가이드의
 * "Use a single UI state object to handle states that are related to each other" 다.
 * 쪼개는 편이 나은 경우는 가이드가 함께 못박은 예외 "Unrelated data types"(서로 독립적이고
 * 갱신 빈도까지 다른 상태)인데, 이 폼은 카테고리 선택·검증·저장 payload 가 서로를 읽으므로 해당하지 않는다.
 * https://developer.android.com/topic/architecture/ui-layer#define-ui-state
 *
 * [com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorViewModel]의
 * [kotlinx.coroutines.flow.StateFlow]가 SSOT이며,
 * 프로세스 종료 대비 스냅샷은 [androidx.lifecycle.SavedStateHandle]에 JSON으로 저장한다.
 *
 * 순수 UI(다이얼로그·탭·드롭다운·[androidx.compose.foundation.text.input.TextFieldState])는
 * [AfternoteEditorUiHolder]가 담당한다.
 *
 * **남기실 말씀:** [leaveMessageBlocks]는 SavedState 스냅샷·Process Death 복원용 SSOT이며,
 * 화면의 [androidx.compose.foundation.text.input.TextFieldState]와 디바운스 동기화된다.
 *
 * **추억 플레이리스트:** [memorialPlaylistSongs]는 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] 와
 * 동기화되어 [androidx.lifecycle.SavedStateHandle] JSON에 포함된다 (프로세스 종료·설정 변경 복원).
 *
 * **Bundle 용량:** 스냅샷이 들어가는 SavedState/번들은 대략 500KB~1MB를 넘기면 [android.os.TransactionTooLargeException] 위험이 있다.
 * 사진·썸네일은 Base64/data URL 같은 거대 문자열이 아니라 짧은 HTTPS URL 또는 content [android.net.Uri] 문자열만 두는 것이 안전하다.
 *
 * **서비스명:** [selectedService]의 `null`은 미선택(드롭다운 placeholder 노출) 상태이며, 등록 검증에서 차단된다.
 *
 * **처리 방법 리스트:** [processingMethods]는 계정 폼(소셜·비즈니스)과 갤러리 폼이 공유하는 단일 리스트다.
 * 화면엔 어느 시점에도 한 카테고리의 섹션만 렌더되고 카테고리 전환 시 리셋되므로([selectedService] 리셋과 같은 정책),
 * 카테고리별로 리스트를 나눌 이유가 없다.
 */
data class EditorFormState(
    val loadedItemId: String? = null,
    val selectedCategory: EditorCategory = EditorCategory.SOCIAL,
    val selectedService: String? = null,
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val processingMethods: List<ProcessingMethodItem> = emptyList(),
    val pickedMemorialPhotoUri: String? = null,
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    /** 추모(PLAYLIST) 곡 목록 — 홀더와 양방향 동기화 후 스냅샷에 저장. */
    val memorialPlaylistSongs: List<Song> = emptyList(),
    /** 저장·복원용 남기실 말씀 블록 (화면 TextField와 주기적으로 맞춘다). */
    val leaveMessageBlocks: List<EditorMessageTextBlock> = DEFAULT_EDITOR_MESSAGE_BLOCKS,
    /**
     * 0이 아니면 SavedState 등에서 폼이 복원된 뒤 UI에 블록을 한 번 밀어 넣어야 함을 뜻한다.
     * 타이핑 동기화(debounce)로 갱신할 때는 바꾸지 않는다.
     */
    val leaveMessageBlocksRestoreGeneration: Long = 0L,
) {
    fun displayMemorialPhotoUri(): String? = pickedMemorialPhotoUri ?: memorialPhotoUrl

    /**
     * 그래프 스코프 SSOT([com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs])의
     * 곡 목록을 앨범 커버로 변환한다. 곡 수 표시도 이 목록의 크기를 쓴다 — 목록과 개수가 어긋날 수 있는
     * 별도 카운트 필드는 두지 않는다.
     */
    fun displayAlbumCovers(liveSongs: List<Song>): List<AlbumCover> =
        liveSongs.map { s ->
            AlbumCover(id = s.id, imageUrl = s.albumCoverUrl, title = s.title)
        }

    val currentServiceOptions: List<String>
        get() =
            when (selectedCategory) {
                EditorCategory.SOCIAL -> AfternoteServiceCatalog.socialServices + CUSTOM_ADD_OPTION

                EditorCategory.GALLERY -> AfternoteServiceCatalog.galleryServices

                // 비즈니스는 직접 추가 미제공 — 시안(700:38735)에 드롭다운 펼침·직접 추가 항목이 없다 (Ready for dev 07-01 실측).
                EditorCategory.BUSINESS -> AfternoteServiceCatalog.businessServices

                // [EditorCategory.hasServiceSelection]=false 로 드롭다운이 렌더되지 않는 동안만 유효한 자리 채움.
                // 두 카테고리의 서비스명 UI 는 미확정 상태다(ESTATE: "제목만 구현" 보류, MEMORIAL: 현행 에디터 정본 시안 부재) —
                // 구현이 열리면 hasServiceSelection 과 이 분기를 함께 갱신할 것.
                EditorCategory.MEMORIAL, EditorCategory.ESTATE -> emptyList()
            }

    fun isCustomAddOption(service: String): Boolean = service == CUSTOM_ADD_OPTION
}
