package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

internal const val CUSTOM_ADD_OPTION = "직접 추가하기"

internal val DEFAULT_EDITOR_MESSAGE_BLOCKS: List<EditorMessageTextBlock> =
    listOf(EditorMessageTextBlock(title = "", body = ""))

/**
 * 에디터 **비즈니스/도메인** 폼 상태.
 *
 * 전 카테고리 공용 필드만 직접 들고, 카테고리 전용 입력은 [categoryForm] 으로 분리한다 —
 * UI 레이어 가이드가 단일 상태 객체의 예외로 못박은 "Unrelated data types" 에 해당한다.
 * 이 분리로 카테고리 전환 시 이전 값 잔류가 구조적으로 불가능해진다 (선행 사고 #213).
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
 * 현재 MEMORIAL 화면은 이 섹션을 렌더하지 않지만 전 카테고리 공용 자리에 둔다 — 프리필 읽기 경로가
 * 이미 카테고리와 무관하고, 추억 노트에도 이 입력을 추가하는 작업(#678)이 예정돼 있다.
 *
 * **서비스명:** [selectedService]의 `null`은 미선택(드롭다운 placeholder 노출) 상태이며, 등록 검증에서 차단된다.
 */
data class EditorFormState(
    val loadedItemId: String? = null,
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    /** 저장·복원용 남기실 말씀 블록 (화면 TextField와 주기적으로 맞춘다). */
    val leaveMessageBlocks: List<EditorMessageTextBlock> = DEFAULT_EDITOR_MESSAGE_BLOCKS,
    /**
     * 0이 아니면 SavedState 등에서 폼이 복원된 뒤 UI에 블록을 한 번 밀어 넣어야 함을 뜻한다.
     * 타이핑 동기화(debounce)로 갱신할 때는 바꾸지 않는다.
     */
    val leaveMessageBlocksRestoreGeneration: Long = 0L,
    val categoryForm: CategoryForm = CategoryForm.Social(),
) {
    val selectedCategory: EditorCategory get() = categoryForm.category

    val selectedService: String? get() = (categoryForm as? CategoryForm.WithServiceAndProcessingMethods)?.selectedService

    val processingMethods: List<ProcessingMethodItem>
        get() = (categoryForm as? CategoryForm.WithServiceAndProcessingMethods)?.processingMethods.orEmpty()

    val memorialForm: CategoryForm.Memorial? get() = categoryForm as? CategoryForm.Memorial

    val pickedMemorialPhotoUri: String? get() = memorialForm?.pickedPhotoUri
    val memorialVideoUrl: String? get() = memorialForm?.videoUrl
    val memorialThumbnailUrl: String? get() = memorialForm?.thumbnailUrl
    val memorialPhotoUrl: String? get() = memorialForm?.photoUrl
    val memorialPlaylistSongs: List<Song> get() = memorialForm?.playlistSongs.orEmpty()

    fun displayMemorialPhotoUri(): String? = memorialForm?.displayPhotoUri()

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
