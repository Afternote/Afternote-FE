package com.afternote.feature.afternote.presentation.author.editor.state

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
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCredentialsPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

private const val TAG = "AfternoteEditorState"

/**
 * 에디터 화면용 **안정적인 파사드**: ViewModel의 폼 스냅샷([getCurrentForm])과 폼 갱신 인텐트 콜백을 받아
 * UI 측 [TextFieldState]·이펙트와 결합한다. 컴포지션 스코프 내에서만 생성되며, 그래프 스코프 ViewModel에 캐싱하지 않는다.
 *
 * 폼을 임의로 변환하는 통로(`(EditorFormState) -> EditorFormState` 람다)는 받지 않는다 — 어떤 필드를 어떻게
 * 바꿀지는 데이터 소유자가 정하고, 파사드는 무슨 일이 일어났는지만 개별 콜백으로 통보한다
 * ([State holders](https://developer.android.com/topic/architecture/ui-layer/stateholders) 의
 * *"pass only the information it needs as a parameter"*). 그중 UI 로직이 붙지 않는 것은 콜백을 그대로 공개한다.
 *
 * 콜백 메서드들은 [getCurrentForm] 으로 최신 폼 스냅샷을 읽는다 (stale closure 회피).
 * 추모 곡 목록은 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] 가 SSOT 이므로
 * 본 파사드는 곡 목록을 직접 보유·참조하지 않는다.
 */
@Stable
class AfternoteEditorState(
    private val ui: AfternoteEditorUiHolder,
    private val getCurrentForm: () -> EditorFormState,
    private val setCategory: (EditorCategory) -> Unit,
    private val setService: (String) -> Unit,
    private val addReceiverIfAbsent: (receiverId: String, name: String, label: String) -> Unit,
    private val applyPrefill: (EditorFormPrefill) -> Unit,
    /** 영정 사진·추모 영상 picker 결과 (`Uri?.toString()`). */
    val setMemorialPhoto: (String?) -> Unit,
    val setMemorialVideo: (String?) -> Unit,
    /** 추모 영상 썸네일 추출 완료 — 폼에 그대로 반영한다. */
    val setMemorialThumbnail: (String?) -> Unit,
    /** 호스트 SSOT의 곡 목록을 폼 스냅샷에 반영한다 (SavedStateHandle JSON에 포함하기 위함). */
    val setMemorialPlaylistSongs: (List<Song>) -> Unit,
    val deleteReceiver: (receiverId: String) -> Unit,
    val replaceReceiversIfEmpty: (List<AfternoteEditorReceiver>) -> Unit,
    /** 타이핑 디바운스 후 폼(및 스냅샷)에만 반영; [EditorFormState.leaveMessageBlocksRestoreGeneration]은 건드리지 않는다. */
    val setLeaveMessageBlocks: (List<EditorMessageTextBlock>) -> Unit,
    val addProcessingMethod: (text: String) -> Unit,
    val deleteProcessingMethod: (localId: Int) -> Unit,
    val editProcessingMethod: (localId: Int, newText: String) -> Unit,
) {
    val editorMessages: SnapshotStateList<EditorMessage> get() = ui.editorMessages

    val idState: TextFieldState get() = ui.idState
    val passwordState: TextFieldState get() = ui.passwordState
    val customServiceNameState: TextFieldState get() = ui.customServiceNameState

    val activeDialog get() = ui.activeDialog
    val categoryDropdownExpanded get() = ui.categoryDropdownExpanded
    val serviceDropdownExpanded get() = ui.serviceDropdownExpanded

    /** 콜백·payload 조립 등 일회성 read 용 (Compose 표시는 화면이 collect 한 `uiState.form` 사용). */
    fun currentForm(): EditorFormState = getCurrentForm()

    fun onCategoryDropdownExpandedChange(expanded: Boolean) = ui.onCategoryDropdownExpandedChange(expanded)

    fun onServiceDropdownExpandedChange(expanded: Boolean) = ui.onServiceDropdownExpandedChange(expanded)

    /** 드롭다운 UI에서 [categoryDisplayLabel] 문자열로 카테고리를 선택한다. */
    fun onCategorySelected(categoryDisplayLabel: String) {
        setCategory(EditorCategory.fromDisplayLabel(categoryDisplayLabel))
    }

    /** 네비게이션 인자([EditorCategory.name])로 카테고리를 선택한다. */
    fun selectCategoryByNavKey(navKey: String) {
        setCategory(EditorCategory.fromNavKey(navKey))
    }

    fun onServiceSelected(service: String) {
        if (getCurrentForm().isCustomAddOption(service)) {
            ui.showCustomServiceDialog()
        } else {
            setService(service)
        }
    }

    fun dismissDialog() = ui.dismissDialog()

    fun onAddCustomService() {
        val serviceName =
            ui.customServiceNameState.text
                .toString()
                .trim()
        if (serviceName.isEmpty()) return
        setService(serviceName)
        dismissDialog()
    }

    fun addReceiverById(
        receiverId: Long,
        name: String,
        relation: String,
    ) {
        addReceiverIfAbsent(receiverId.toString(), name, relation)
    }

    /**
     * 추가·삭제 모두 UI 목록 전체를 폼에 덮어쓴다. 폼은 디바운스로 갱신돼 UI 보다 오래됐을 수 있어,
     * 폼 기준으로 증분 반영하면 마지막 디바운스 이전 입력이 밀려난다.
     */
    fun addEditorMessage() {
        ui.addEditorMessage()
        setLeaveMessageBlocks(ui.editorMessages.toTextBlocks())
    }

    fun removeEditorMessage(message: EditorMessage) {
        if (ui.editorMessages.size <= 1) return
        ui.removeEditorMessage(message)
        setLeaveMessageBlocks(ui.editorMessages.toTextBlocks())
    }

    /** SavedState·프리필·재진입 등 폼 SSOT → TextField 목록 반영. */
    fun syncEditorMessagesFromForm(blocks: List<EditorMessageTextBlock>) {
        val normalized = normalizeEditorMessageBlocks(blocks)
        ui.editorMessages.clear()
        for (b in normalized) {
            val msg = EditorMessage()
            msg.titleState.edit { replace(0, length, b.title) }
            msg.contentState.edit { replace(0, length, b.body) }
            ui.editorMessages.add(msg)
        }
    }

    /**
     * ViewModel이 [EditorFormPrefill]을 적용할 때 호출. 비즈니스 필드는 [EditorFormState]로, 메시지·계정 텍스트는 UI에 반영.
     * 추모 곡 목록은 host VM이 SSOT이므로 본 메서드는 곡 목록을 폼 스냅샷에만 채우고, 호스트 동기화는 호출자가 수행한다.
     */
    fun applyFormPrefill(prefill: EditorFormPrefill) {
        Log.d(
            TAG,
            "applyFormPrefill: itemId=${prefill.loadedItemId}, type=${prefill.type}",
        )
        applyPrefill(prefill)
        val credentials: EditorCredentialsPrefill? =
            when (val content = prefill.content) {
                is EditorContentPrefill.SocialNetwork -> content.credentials

                is EditorContentPrefill.Business -> content.credentials

                is EditorContentPrefill.Gallery,
                is EditorContentPrefill.Memorial,
                EditorContentPrefill.Estate,
                -> null
            }
        ui.idState.edit { replace(0, length, credentials?.id.orEmpty()) }
        ui.passwordState.edit { replace(0, length, credentials?.password.orEmpty()) }
        syncEditorMessagesFromForm(prefill.leaveMessageBlocks)
    }
}

private fun List<EditorMessage>.toTextBlocks(): List<EditorMessageTextBlock> =
    map { m ->
        EditorMessageTextBlock(
            title = m.titleState.text.toString(),
            body = m.contentState.text.toString(),
        )
    }

/**
 * 프로덕션용 팩토리.
 *
 * ViewModel의 폼 SSOT 스냅샷을 [getCurrentForm] 클로저로, 폼 갱신 인텐트를 개별 콜백으로 받아
 * UI 레이어가 소유한 [TextFieldState]와 [AfternoteEditorUiHolder]를 결합한 파사드를 만든다. ViewModel은
 * Compose UI 상태를 들지 않으므로, 이 팩토리는 반드시 Composable 스코프에서 호출되어야 한다.
 */
@Composable
fun rememberAfternoteEditorState(
    getCurrentForm: () -> EditorFormState,
    setCategory: (EditorCategory) -> Unit,
    setService: (String) -> Unit,
    setMemorialPhoto: (String?) -> Unit,
    setMemorialVideo: (String?) -> Unit,
    addReceiverIfAbsent: (receiverId: String, name: String, label: String) -> Unit,
    applyPrefill: (EditorFormPrefill) -> Unit,
    setMemorialThumbnail: (String?) -> Unit,
    setMemorialPlaylistSongs: (List<Song>) -> Unit,
    deleteReceiver: (receiverId: String) -> Unit,
    replaceReceiversIfEmpty: (List<AfternoteEditorReceiver>) -> Unit,
    setLeaveMessageBlocks: (List<EditorMessageTextBlock>) -> Unit,
    addProcessingMethod: (text: String) -> Unit,
    deleteProcessingMethod: (localId: Int) -> Unit,
    editProcessingMethod: (localId: Int, newText: String) -> Unit,
): AfternoteEditorState {
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val customServiceNameState = rememberTextFieldState()

    val ui =
        rememberAfternoteEditorUiHolder(
            idState = idState,
            passwordState = passwordState,
            customServiceNameState = customServiceNameState,
        )

    return remember(ui) {
        AfternoteEditorState(
            ui = ui,
            getCurrentForm = getCurrentForm,
            setCategory = setCategory,
            setService = setService,
            setMemorialPhoto = setMemorialPhoto,
            setMemorialVideo = setMemorialVideo,
            addReceiverIfAbsent = addReceiverIfAbsent,
            applyPrefill = applyPrefill,
            setMemorialThumbnail = setMemorialThumbnail,
            setMemorialPlaylistSongs = setMemorialPlaylistSongs,
            deleteReceiver = deleteReceiver,
            replaceReceiversIfEmpty = replaceReceiversIfEmpty,
            setLeaveMessageBlocks = setLeaveMessageBlocks,
            addProcessingMethod = addProcessingMethod,
            deleteProcessingMethod = deleteProcessingMethod,
            editProcessingMethod = editProcessingMethod,
        )
    }
}

/**
 * Compose Preview·로컬 UI 테스트 전용. 내부 [androidx.compose.runtime.MutableState]로 자체 폼 SSOT를 만들고
 * 프로덕션 ViewModel과 같은 `EditorFormMutations.kt` 의 변환 규칙으로 갱신한다.
 */
@Composable
fun rememberAfternoteEditorState(): AfternoteEditorState {
    val previewForm = remember { mutableStateOf(EditorFormState()) }
    val mutate: ((EditorFormState) -> EditorFormState) -> Unit =
        { block -> previewForm.value = block(previewForm.value) }
    return rememberAfternoteEditorState(
        getCurrentForm = { previewForm.value },
        setCategory = { category -> mutate { it.withCategory(category) } },
        setService = { service -> mutate { it.withService(service) } },
        setMemorialPhoto = { uri -> mutate { it.withMemorialPhoto(uri) } },
        setMemorialVideo = { url -> mutate { it.withMemorialVideo(url) } },
        addReceiverIfAbsent = { receiverId, name, label ->
            mutate { it.withReceiverAddedIfAbsent(receiverId = receiverId, name = name, label = label) }
        },
        applyPrefill = { prefill -> mutate { it.withPrefillApplied(prefill) } },
        setMemorialThumbnail = { dataUrl -> mutate { it.withMemorialThumbnail(dataUrl) } },
        setMemorialPlaylistSongs = { songs -> mutate { it.withMemorialPlaylistSongs(songs) } },
        deleteReceiver = { receiverId -> mutate { it.withReceiverDeleted(receiverId) } },
        replaceReceiversIfEmpty = { receivers -> mutate { it.withReceiversReplacedIfEmpty(receivers) } },
        setLeaveMessageBlocks = { blocks -> mutate { it.withLeaveMessageBlocks(blocks) } },
        addProcessingMethod = { text -> mutate { it.withProcessingMethodAdded(text) } },
        deleteProcessingMethod = { itemId -> mutate { it.withProcessingMethodDeleted(itemId) } },
        editProcessingMethod = { localId, newText ->
            mutate { it.withProcessingMethodEdited(localId = localId, newText = newText) }
        },
    )
}
