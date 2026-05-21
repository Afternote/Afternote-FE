package com.afternote.feature.afternote.presentation.author.editor.state

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodCallbacks
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

private const val TAG = "AfternoteEditorState"

private fun normalizeEditorMessageBlocks(blocks: List<EditorMessageTextBlock>): List<EditorMessageTextBlock> =
    blocks.ifEmpty { DEFAULT_EDITOR_MESSAGE_BLOCKS }

/**
 * 에디터 화면용 **안정적인 파사드**: ViewModel의 단일 [AfternoteEditorUiHolder] SSOT 안의
 * 폼 스냅샷([getCurrentForm])과 폼 갱신 인텐트([updateForm])를 받아 UI 측 [TextFieldState]·이펙트와 결합한다.
 * 컴포지션 스코프 내에서만 생성되며, 그래프 스코프 ViewModel에 캐싱하지 않는다.
 *
 * 화면은 ViewModel의 단일 `uiState` 만 collect 하고 그 안의 `form` 을 렌더링에 쓰며,
 * 본 파사드의 콜백 메서드들은 [getCurrentForm] 으로 최신 폼 스냅샷을 읽는다 (콜백은 stale closure 회피).
 *
 * 추모 곡 목록은 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] 가 SSOT 이므로
 * 본 파사드는 곡 목록을 직접 보유·참조하지 않으며, 곡 변경은 host VM 인텐트로 처리한다.
 */
@Stable
class AfternoteEditorState(
    val ui: AfternoteEditorUiHolder,
    private val getCurrentForm: () -> EditorFormState,
    private val updateForm: ((EditorFormState) -> EditorFormState) -> Unit,
) {
    val editorMessages: SnapshotStateList<EditorMessage> get() = ui.editorMessages

    val idState: TextFieldState get() = ui.idState
    val passwordState: TextFieldState get() = ui.passwordState
    val afternoteEditReceiverNameState: TextFieldState get() = ui.afternoteEditReceiverNameState
    val phoneNumberState: TextFieldState get() = ui.phoneNumberState
    val customServiceNameState: TextFieldState get() = ui.customServiceNameState
    val customLastWishState: TextFieldState get() = ui.customLastWishState

    val activeDialog get() = ui.activeDialog
    val relationshipSelectedValue get() = ui.relationshipSelectedValue
    val categoryDropdownExpanded get() = ui.categoryDropdownExpanded
    val serviceDropdownExpanded get() = ui.serviceDropdownExpanded

    /** 콜백·payload 조립 등 일회성 read 용 (Compose 표시는 화면이 collect 한 `uiState.form` 사용). */
    fun currentForm(): EditorFormState = getCurrentForm()

    val selectedCategory get() = getCurrentForm().selectedCategory
    val funeralVideoUrl get() = getCurrentForm().funeralVideoUrl
    val funeralThumbnailUrl get() = getCurrentForm().funeralThumbnailUrl
    val memorialPhotoUrl get() = getCurrentForm().memorialPhotoUrl
    val pickedMemorialPhotoUri get() = getCurrentForm().pickedMemorialPhotoUri
    val afternoteEditReceivers get() = getCurrentForm().afternoteEditReceivers

    val galleryProcessingCallbacks: ProcessingMethodCallbacks =
        ProcessingMethodCallbacks(
            onItemDeleteClick = ::deleteGalleryProcessingMethod,
            onItemAdded = ::addGalleryProcessingMethod,
            onTextFieldVisibilityChanged = { },
            onItemEdited = ::editGalleryProcessingMethod,
        )

    val socialProcessingCallbacks: ProcessingMethodCallbacks =
        ProcessingMethodCallbacks(
            onItemDeleteClick = ::deleteProcessingMethod,
            onItemAdded = ::addProcessingMethod,
            onTextFieldVisibilityChanged = { },
            onItemEdited = ::editProcessingMethod,
        )

    fun onCategoryDropdownExpandedChange(expanded: Boolean) = ui.onCategoryDropdownExpandedChange(expanded)

    fun onServiceDropdownExpandedChange(expanded: Boolean) = ui.onServiceDropdownExpandedChange(expanded)

    /** 호스트 SSOT의 곡 목록을 폼 스냅샷으로 동기화한다 (SavedStateHandle JSON에 포함하기 위함). */
    fun syncMemorialPlaylistSongs(songs: List<Song>) {
        updateForm {
            it.copy(
                memorialPlaylistSongs = songs,
                playlistSongCount = if (songs.isNotEmpty()) songs.size else it.playlistSongCount,
            )
        }
    }

    /** 신규 작성 진입 시 폼에 남은 추모 플레이리스트 스냅샷을 비운다 (호스트 SSOT clear는 호출부에서). */
    fun resetMemorialPlaylistFormSnapshot() {
        updateForm {
            it.copy(memorialPlaylistSongs = emptyList(), playlistSongCount = 16)
        }
    }

    /** 드롭다운 UI에서 [categoryDisplayLabel] 문자열로 카테고리를 선택한다. */
    fun onCategorySelected(categoryDisplayLabel: String) {
        selectCategory(EditorCategory.fromDisplayLabel(categoryDisplayLabel))
    }

    /** 네비게이션 인자([EditorCategory.name])로 카테고리를 선택한다. */
    fun selectCategoryByNavKey(navKey: String) {
        selectCategory(EditorCategory.fromNavKey(navKey))
    }

    private fun selectCategory(category: EditorCategory) {
        updateForm {
            it.copy(
                selectedCategory = category,
                selectedService =
                    if (category == EditorCategory.GALLERY) {
                        AfternoteServiceCatalog.defaultGalleryService
                    } else {
                        AfternoteServiceCatalog.defaultSocialService
                    },
                socialProcessingMethods = emptyList(),
                galleryProcessingMethods = emptyList(),
            )
        }
    }

    fun onServiceSelected(service: String) {
        if (getCurrentForm().isCustomAddOption(service)) {
            ui.showCustomServiceDialog()
        } else {
            updateForm { it.copy(selectedService = service) }
        }
    }

    fun onLastWishSelected(wish: String?) {
        updateForm { it.copy(selectedLastWish = wish) }
    }

    fun getAtmosphereForSave(): String = getCurrentForm().atmosphereForSave(ui.customLastWishState.text.toString())

    fun onMemorialPhotoSelected(uri: Uri?) {
        updateForm { it.copy(pickedMemorialPhotoUri = uri?.toString()) }
    }

    fun onFuneralVideoSelected(uri: Uri?) {
        updateForm { it.copy(funeralVideoUrl = uri?.toString(), funeralThumbnailUrl = null) }
    }

    fun onFuneralThumbnailDataUrlReady(dataUrl: String?) {
        updateForm { it.copy(funeralThumbnailUrl = dataUrl) }
    }

    fun showAddAfternoteEditorReceiverDialog() = ui.showAddAfternoteEditorReceiverDialog()

    fun dismissDialog() = ui.dismissDialog()

    fun onAddCustomService() {
        val serviceName =
            ui.customServiceNameState.text
                .toString()
                .trim()
        if (serviceName.isEmpty()) return
        updateForm { it.copy(selectedService = serviceName) }
        dismissDialog()
    }

    fun onAddAfternoteEditorReceiver() {
        val name =
            ui.afternoteEditReceiverNameState.text
                .toString()
                .trim()
        if (name.isEmpty()) return
        updateForm { prev ->
            val next =
                AfternoteEditorReceiver(
                    id = (prev.afternoteEditReceivers.size + 1).toString(),
                    name = name,
                    label = ui.relationshipSelectedValue,
                )
            prev.copy(afternoteEditReceivers = prev.afternoteEditReceivers + next)
        }
        dismissDialog()
    }

    fun onRelationshipSelected(relationship: String) = ui.onRelationshipSelected(relationship)

    fun onAfternoteEditorReceiverDelete(afternoteEditReceiverId: String) {
        updateForm { prev ->
            prev.copy(
                afternoteEditReceivers =
                    prev.afternoteEditReceivers.filter { it.id != afternoteEditReceiverId },
            )
        }
    }

    fun addReceiverFromSelection(
        receiverId: Long,
        name: String,
        relation: String,
    ) {
        updateForm { prev ->
            if (prev.afternoteEditReceivers.any { it.id == receiverId.toString() }) return@updateForm prev
            val newReceiver =
                AfternoteEditorReceiver(
                    id = receiverId.toString(),
                    name = name,
                    label = relation,
                )
            prev.copy(afternoteEditReceivers = prev.afternoteEditReceivers + newReceiver)
        }
    }

    fun replaceReceiversIfEmpty(receivers: List<AfternoteEditorReceiver>) {
        if (receivers.isEmpty()) return
        updateForm { prev ->
            if (prev.afternoteEditReceivers.isNotEmpty()) return@updateForm prev
            prev.copy(afternoteEditReceivers = receivers)
        }
    }

    fun onAfternoteEditorReceiverItemAdded(text: String) {
        updateForm { prev ->
            val newReceiver =
                AfternoteEditorReceiver(
                    id = (prev.afternoteEditReceivers.size + 1).toString(),
                    name = text,
                    label = "친구",
                )
            prev.copy(afternoteEditReceivers = prev.afternoteEditReceivers + newReceiver)
        }
    }

    fun addEditorMessage() {
        ui.addEditorMessage()
        updateForm { prev ->
            prev.copy(
                messageBlocks = prev.messageBlocks + EditorMessageTextBlock(title = "", body = ""),
            )
        }
    }

    fun removeEditorMessage(message: EditorMessage) {
        if (ui.editorMessages.size <= 1) return
        ui.removeEditorMessage(message)
        updateForm { prev ->
            prev.copy(
                messageBlocks =
                    normalizeEditorMessageBlocks(
                        ui.editorMessages.map { m ->
                            EditorMessageTextBlock(
                                title = m.titleState.text.toString(),
                                body = m.contentState.text.toString(),
                            )
                        },
                    ),
            )
        }
    }

    private fun applyMessageBlocks(blocks: List<EditorMessageTextBlock>) {
        val normalized = normalizeEditorMessageBlocks(blocks)
        ui.editorMessages.clear()
        for (b in normalized) {
            val msg = EditorMessage()
            msg.titleState.edit { replace(0, length, b.title) }
            msg.contentState.edit { replace(0, length, b.body) }
            ui.editorMessages.add(msg)
        }
    }

    /** SavedState·프리필·재진입 등 폼 SSOT → TextField 목록 반영. */
    fun syncEditorMessagesFromForm(blocks: List<EditorMessageTextBlock>) {
        applyMessageBlocks(blocks)
    }

    /** 타이핑 디바운스 후 폼(및 스냅샷)에만 반영; [EditorFormState.messageBlocksRestoreGeneration]은 건드리지 않는다. */
    fun persistEditorMessagesFromTyping(blocks: List<EditorMessageTextBlock>) {
        updateForm { it.copy(messageBlocks = normalizeEditorMessageBlocks(blocks)) }
    }

    /**
     * ViewModel이 [EditorFormPrefill]을 적용할 때 호출. 비즈니스 필드는 [EditorFormState]로, 메시지·계정 텍스트는 UI에 반영.
     * 추모 곡 목록은 host VM이 SSOT이므로 본 메서드는 곡 목록을 폼 스냅샷에만 채우고, 호스트 동기화는 호출자가 수행한다.
     */
    fun applyFormPrefill(prefill: EditorFormPrefill) {
        Log.d(
            TAG,
            "applyFormPrefill: itemId=${prefill.loadedItemId}, serviceName=${prefill.serviceName}, " +
                "category=${prefill.category}, " +
                "socialPMs=${prefill.socialProcessingMethods.size}, galleryPMs=${prefill.galleryProcessingMethods.size}",
        )
        val prefillBlocks = normalizeEditorMessageBlocks(prefill.messageBlocks)
        updateForm { prev ->
            val withLastWish =
                prefill.lastWishUpdate?.let { lw ->
                    prev.copy(selectedLastWish = lw.selectedKey)
                } ?: prev
            withLastWish.copy(
                loadedItemId = prefill.loadedItemId,
                selectedCategory = prefill.category,
                selectedService = prefill.serviceName,
                socialProcessingMethods = prefill.socialProcessingMethods,
                galleryProcessingMethods = prefill.galleryProcessingMethods,
                funeralVideoUrl = prefill.funeralVideoUrl,
                funeralThumbnailUrl = prefill.funeralThumbnailUrl,
                memorialPhotoUrl = prefill.memorialPhotoUrl,
                memorialPlaylistSongs = prefill.memorialPlaylistSongs,
                playlistSongCount =
                    if (prefill.memorialPlaylistSongs.isNotEmpty()) {
                        prefill.memorialPlaylistSongs.size
                    } else {
                        withLastWish.playlistSongCount
                    },
                messageBlocks = prefillBlocks,
            )
        }
        ui.idState.edit { replace(0, length, prefill.accountId) }
        ui.passwordState.edit { replace(0, length, prefill.password) }
        applyMessageBlocks(prefillBlocks)
        prefill.lastWishUpdate?.let { lw ->
            ui.customLastWishState.edit { replace(0, length, lw.customText) }
        }
    }

    private fun addProcessingMethod(text: String) {
        updateForm { prev ->
            val newItem =
                ProcessingMethodItem(
                    id = (prev.socialProcessingMethods.size + 1).toString(),
                    text = text,
                )
            prev.copy(socialProcessingMethods = prev.socialProcessingMethods + newItem)
        }
    }

    private fun deleteProcessingMethod(itemId: String) {
        updateForm { prev ->
            prev.copy(socialProcessingMethods = prev.socialProcessingMethods.filter { it.id != itemId })
        }
    }

    private fun editProcessingMethod(
        itemId: String,
        newText: String,
    ) {
        updateForm { prev ->
            prev.copy(
                socialProcessingMethods =
                    prev.socialProcessingMethods.map { item ->
                        if (item.id == itemId) item.copy(text = newText) else item
                    },
            )
        }
    }

    private fun addGalleryProcessingMethod(text: String) {
        updateForm { prev ->
            val newItem =
                ProcessingMethodItem(
                    id = (prev.galleryProcessingMethods.size + 1).toString(),
                    text = text,
                )
            prev.copy(galleryProcessingMethods = prev.galleryProcessingMethods + newItem)
        }
    }

    private fun deleteGalleryProcessingMethod(itemId: String) {
        updateForm { prev ->
            prev.copy(galleryProcessingMethods = prev.galleryProcessingMethods.filter { it.id != itemId })
        }
    }

    private fun editGalleryProcessingMethod(
        itemId: String,
        newText: String,
    ) {
        updateForm { prev ->
            prev.copy(
                galleryProcessingMethods =
                    prev.galleryProcessingMethods.map { item ->
                        if (item.id == itemId) item.copy(text = newText) else item
                    },
            )
        }
    }
}

/**
 * 프로덕션용 팩토리.
 *
 * ViewModel의 폼 SSOT 스냅샷을 [getCurrentForm] 클로저로, `updateForm` 인텐트를 [updateForm] 으로 받아
 * UI 레이어가 소유한 [TextFieldState]와 [AfternoteEditorUiHolder]를 결합한 파사드를 만든다. ViewModel은
 * Compose UI 상태를 들지 않으므로, 이 팩토리는 반드시 Composable 스코프에서 호출되어야 한다.
 */
@Composable
fun rememberAfternoteEditorState(
    getCurrentForm: () -> EditorFormState,
    updateForm: ((EditorFormState) -> EditorFormState) -> Unit,
): AfternoteEditorState {
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val afternoteEditReceiverNameState = rememberTextFieldState()
    val phoneNumberState = rememberTextFieldState()
    val customServiceNameState = rememberTextFieldState()
    val customLastWishState = rememberTextFieldState()

    val ui =
        rememberAfternoteEditorUiHolder(
            idState = idState,
            passwordState = passwordState,
            afternoteEditReceiverNameState = afternoteEditReceiverNameState,
            phoneNumberState = phoneNumberState,
            customServiceNameState = customServiceNameState,
            customLastWishState = customLastWishState,
        )

    return remember(ui) {
        AfternoteEditorState(
            ui = ui,
            getCurrentForm = getCurrentForm,
            updateForm = updateForm,
        )
    }
}

/**
 * Compose Preview·로컬 UI 테스트 전용. 내부 [androidx.compose.runtime.MutableState]로 자체 폼 SSOT를 만든다.
 */
@Composable
fun rememberAfternoteEditorState(): AfternoteEditorState {
    val previewForm = remember { mutableStateOf(EditorFormState()) }
    return rememberAfternoteEditorState(
        getCurrentForm = { previewForm.value },
        updateForm = { block -> previewForm.value = block(previewForm.value) },
    )
}
