package com.afternote.feature.afternote.presentation.author.editor.state

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.afternote.core.model.AlbumCover
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodCallbacks
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiverCallbacks
import com.afternote.feature.afternote.presentation.shared.LastWishOption
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "AfternoteEditorState"

private fun normalizeEditorMessageBlocks(blocks: List<EditorMessageTextBlock>): List<EditorMessageTextBlock> =
    blocks.ifEmpty { DEFAULT_EDITOR_MESSAGE_BLOCKS }

/**
 * 에디터 화면용 **안정적인 파사드**: [formState]는 ViewModel(또는 프리뷰용 [MutableStateFlow])의 SSOT를 노출하고,
 * [ui]는 순수 UI 상태를 둔다. 네비게이션·호스트는 이 인스턴스 참조를 그대로 유지할 수 있다.
 *
 * 읽기: Composable에서 `val form by state.formState.collectAsStateWithLifecycle()` 후 `form`과 `state.ui`를 조합한다.
 */
@Stable
class AfternoteEditorState(
    val ui: AfternoteEditorUiState,
    private val updateForm: ((EditorFormState) -> EditorFormState) -> Unit,
    formStateSource: StateFlow<EditorFormState>,
) {
    val formState: StateFlow<EditorFormState> = formStateSource

    val editorMessages: SnapshotStateList<EditorMessage>
        get() = ui.editorMessages

    val idState: TextFieldState get() = ui.idState
    val passwordState: TextFieldState get() = ui.passwordState
    val afternoteEditReceiverNameState: TextFieldState get() = ui.afternoteEditReceiverNameState
    val phoneNumberState: TextFieldState get() = ui.phoneNumberState
    val customServiceNameState: TextFieldState get() = ui.customServiceNameState
    val customLastWishState: TextFieldState get() = ui.customLastWishState

    val activeDialog get() = ui.activeDialog
    val selectedBottomNavItem get() = ui.selectedBottomNavItem
    val relationshipSelectedValue get() = ui.relationshipSelectedValue
    val categoryDropdownState get() = ui.categoryDropdownState
    val serviceDropdownState get() = ui.serviceDropdownState
    val playlistStateHolder get() = ui.playlistStateHolder

    /** 콜백·일회성 읽기용. Compose 표시는 [formState]를 collect한 스냅샷을 쓰는 것이 안전하다. */
    val selectedCategory get() = formState.value.selectedCategory
    val selectedService get() = formState.value.selectedService
    val loadedItemId get() = formState.value.loadedItemId
    val funeralVideoUrl get() = formState.value.funeralVideoUrl
    val funeralThumbnailUrl get() = formState.value.funeralThumbnailUrl
    val memorialPhotoUrl get() = formState.value.memorialPhotoUrl
    val pickedMemorialPhotoUri get() = formState.value.pickedMemorialPhotoUri
    val afternoteEditReceivers get() = formState.value.afternoteEditReceivers
    val selectedProcessingMethod get() = formState.value.selectedProcessingMethod
    val selectedInformationProcessingMethod get() = formState.value.selectedInformationProcessingMethod
    val selectedLastWish get() = formState.value.selectedLastWish
    val playlistSongCount get() = formState.value.playlistSongCount
    val displayMemorialPhotoUri get() = formState.value.displayMemorialPhotoUri()
    val livePlaylistSongCount get() = formState.value.livePlaylistSongCount(ui.playlistStateHolder)
    val displayAlbumCovers get() = formState.value.displayAlbumCovers(ui.playlistStateHolder)
    val currentServiceOptions get() = formState.value.currentServiceOptions

    val categories: List<String> = EditorCategory.entries.map { it.displayLabel }
    val services: List<String>
        get() = AfternoteServiceCatalog.socialServices
    val galleryServices: List<String>
        get() = AfternoteServiceCatalog.galleryServices
    val relationshipOptions = listOf("친구", "가족", "연인")
    val lastWishOptions =
        listOf(
            LastWishOption(
                text = "차분하고 조용하게 보내주세요.",
                value = "calm",
            ),
            LastWishOption(
                text = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요.",
                value = "bright",
            ),
            LastWishOption(
                text = "기타(직접 입력)",
                value = "other",
            ),
        )

    val galleryAfternoteEditorReceiverCallbacks: AfternoteEditorReceiverCallbacks by lazy {
        AfternoteEditorReceiverCallbacks(
            onAddClick = ::showAddAfternoteEditorReceiverDialog,
            onItemDeleteClick = ::onAfternoteEditorReceiverDelete,
            onItemAdded = ::onAfternoteEditorReceiverItemAdded,
            onTextFieldVisibilityChanged = { },
        )
    }

    val galleryProcessingCallbacks: ProcessingMethodCallbacks by lazy {
        ProcessingMethodCallbacks(
            onItemDeleteClick = ::deleteGalleryProcessingMethod,
            onItemAdded = ::addGalleryProcessingMethod,
            onTextFieldVisibilityChanged = { },
            onItemEdited = ::editGalleryProcessingMethod,
        )
    }

    val socialProcessingCallbacks: ProcessingMethodCallbacks by lazy {
        ProcessingMethodCallbacks(
            onItemDeleteClick = ::deleteProcessingMethod,
            onItemAdded = ::addProcessingMethod,
            onTextFieldVisibilityChanged = { },
            onItemEdited = ::editProcessingMethod,
        )
    }

    val processingMethods: List<ProcessingMethodItem>
        get() = formState.value.socialProcessingMethods

    val galleryProcessingMethods: List<ProcessingMethodItem>
        get() = formState.value.galleryProcessingMethods

    fun updateAlbumCovers(covers: List<AlbumCover>) {
        updateForm { it.copy(playlistAlbumCovers = covers) }
    }

    fun setPlaylistStateHolder(stateHolder: MemorialPlaylistStateHolder) {
        if (formState.value.selectedCategory == EditorCategory.MEMORIAL &&
            stateHolder.songs.isEmpty() &&
            formState.value.memorialPlaylistSongs.isNotEmpty()
        ) {
            stateHolder.initializeSongs(formState.value.memorialPlaylistSongs)
        }
        ui.setPlaylistStateHolder(stateHolder)
        stateHolder.onSongCountChanged = { syncMemorialPlaylistSongsFromHolder() }
        syncMemorialPlaylistSongsFromHolder()
    }

    private fun syncMemorialPlaylistSongsFromHolder() {
        val holder = ui.playlistStateHolder ?: return
        updateForm {
            it.copy(
                memorialPlaylistSongs = holder.songs.toList(),
                playlistSongCount = holder.songs.size,
            )
        }
    }

    fun updatePlaylistSongCount() {
        syncMemorialPlaylistSongsFromHolder()
    }

    /** 신규 작성 진입 시 폼에 남은 추모 플레이리스트 스냅샷을 비운다 (홀더 clear는 호출부에서). */
    fun resetMemorialPlaylistFormSnapshot() {
        updateForm {
            it.copy(memorialPlaylistSongs = emptyList(), playlistSongCount = 16)
        }
    }

    /** 드롭다운 UI에서 [displayLabel] 문자열로 카테고리를 선택한다. */
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
            )
        }
    }

    fun onServiceSelected(service: String) {
        if (formState.value.isCustomAddOption(service)) {
            showCustomServiceDialog()
        } else {
            updateForm { it.copy(selectedService = service) }
        }
    }

    fun onProcessingMethodSelected(method: AccountProcessingMethod) {
        updateForm { it.copy(selectedProcessingMethod = method) }
    }

    fun onInformationProcessingMethodSelected(method: InformationProcessingMethod) {
        updateForm { it.copy(selectedInformationProcessingMethod = method) }
    }

    fun onLastWishSelected(wish: String?) {
        updateForm { it.copy(selectedLastWish = wish) }
    }

    fun getAtmosphereForSave(): String = formState.value.atmosphereForSave(ui.customLastWishState.text.toString())

    fun onMemorialPhotoSelected(uri: Uri?) {
        updateForm { it.copy(pickedMemorialPhotoUri = uri?.toString()) }
    }

    fun onFuneralVideoSelected(uri: Uri?) {
        updateForm { it.copy(funeralVideoUrl = uri?.toString(), funeralThumbnailUrl = null) }
    }

    fun onFuneralThumbnailDataUrlReady(dataUrl: String?) {
        updateForm { it.copy(funeralThumbnailUrl = dataUrl) }
    }

    fun showAddAfternoteEditorReceiverDialog() {
        ui.showAddAfternoteEditorReceiverDialog()
    }

    fun showCustomServiceDialog() {
        ui.showCustomServiceDialog()
    }

    fun dismissDialog() {
        ui.dismissDialogInternal {
            ui.afternoteEditReceiverNameState.edit { replace(0, length, "") }
            ui.phoneNumberState.edit { replace(0, length, "") }
            ui.customServiceNameState.edit { replace(0, length, "") }
        }
    }

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

    fun onRelationshipSelected(relationship: String) {
        ui.onRelationshipSelected(relationship)
    }

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

    fun onBottomNavItemSelected(item: BottomNavTab) {
        ui.onBottomNavItemSelected(item)
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

    /** SavedState·프리필 등 폼 SSOT → TextField 목록 반영. */
    fun syncEditorMessagesFromForm(blocks: List<EditorMessageTextBlock>) {
        applyMessageBlocks(blocks)
    }

    /** 타이핑 디바운스 후 폼(및 스냅샷)에만 반영; [EditorFormState.messageBlocksRestoreGeneration]은 건드리지 않는다. */
    fun persistEditorMessagesFromTyping(blocks: List<EditorMessageTextBlock>) {
        updateForm { it.copy(messageBlocks = normalizeEditorMessageBlocks(blocks)) }
    }

    /**
     * ViewModel이 [EditorFormPrefill]을 적용할 때 호출. 비즈니스 필드는 [EditorFormState]로, 메시지·계정 텍스트는 UI에 반영.
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
                selectedProcessingMethod =
                    prefill.accountProcessingMethod
                        ?: withLastWish.selectedProcessingMethod,
                selectedInformationProcessingMethod =
                    prefill.informationProcessingMethod
                        ?: withLastWish.selectedInformationProcessingMethod,
                socialProcessingMethods = prefill.socialProcessingMethods,
                galleryProcessingMethods = prefill.galleryProcessingMethods,
                funeralVideoUrl = prefill.funeralVideoUrl,
                funeralThumbnailUrl = prefill.funeralThumbnailUrl,
                memorialPhotoUrl = prefill.memorialPhotoUrl,
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
 * Compose Preview·로컬 UI 테스트 전용.
 */
@Composable
fun rememberAfternoteEditorState(): AfternoteEditorState {
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val afternoteEditReceiverNameState = rememberTextFieldState()
    val phoneNumberState = rememberTextFieldState()
    val customServiceNameState = rememberTextFieldState()
    val customLastWishState = rememberTextFieldState()

    val flow = remember { MutableStateFlow(EditorFormState()) }

    val ui =
        rememberAfternoteEditorUiState(
            idState = idState,
            passwordState = passwordState,
            afternoteEditReceiverNameState = afternoteEditReceiverNameState,
            phoneNumberState = phoneNumberState,
            customServiceNameState = customServiceNameState,
            customLastWishState = customLastWishState,
        )

    return remember(flow, ui) {
        AfternoteEditorState(
            ui = ui,
            updateForm = { block -> flow.update(block) },
            formStateSource = flow.asStateFlow(),
        )
    }
}
