package com.afternote.feature.afternote.presentation.editor.state

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.editor.message.LeaveMessageEditorItem
import com.afternote.feature.afternote.presentation.editor.message.LeaveMessageEditorItemState
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorCredentialsPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver

private const val TAG = "AfternoteEditorState"

/**
 * ViewModel이 소유한 [EditorFormState]와 Compose가 소유한 입력·일시적 UI 상태를 연결한다.
 *
 * 이벤트 처리 시 [getCurrentForm]으로 최신 폼을 읽는다. 이 상태는 컴포지션 안에서 생성하며
 * ViewModel에 저장하지 않는다.
 */
@Stable
class AfternoteEditorState(
    val idState: TextFieldState,
    val passwordState: TextFieldState,
    val serviceSearchQueryState: TextFieldState,
    private val getCurrentForm: () -> EditorFormState,
    private val setType: (AfternoteType) -> Unit,
    private val setService: (String) -> Unit,
    private val addReceiverIfAbsent: (receiverId: Long, name: String, label: String) -> Unit,
    private val applyPrefill: (EditorFormPrefill) -> Unit,
    val setMemorialPhoto: (String) -> Unit,
    val removeMemorialPhoto: () -> Unit,
    val setMemorialVideo: (String) -> Unit,
    val removeMemorialVideo: () -> Unit,
    val setMemorialThumbnail: (String) -> Unit,
    val deleteReceiver: (receiverId: Long) -> Unit,
    val replaceReceiversIfEmpty: (List<AfternoteEditorReceiver>) -> Unit,
    val addProcessingMethod: (text: String) -> Unit,
    val deleteProcessingMethod: (localId: Int) -> Unit,
    val editProcessingMethod: (localId: Int, newText: String) -> Unit,
    val editorMessages: SnapshotStateList<LeaveMessageEditorItem> = mutableStateListOf(),
) {
    var isServiceSelectionSheetVisible by mutableStateOf(false)
        private set

    var typeDropdownExpanded by mutableStateOf(false)
        private set

    /** 이벤트 처리에 사용할 최신 폼. 화면 표시는 수집된 `uiState.form`을 사용한다. */
    fun currentForm(): EditorFormState = getCurrentForm()

    fun onTypeDropdownExpandedChange(expanded: Boolean) {
        typeDropdownExpanded = expanded
    }

    fun onTypeSelected(type: AfternoteType) {
        dismissServiceSelectionSheet()
        setType(type)
    }

    fun openServiceSelectionSheet() {
        isServiceSelectionSheetVisible = true
    }

    fun dismissServiceSelectionSheet() {
        isServiceSelectionSheetVisible = false
        serviceSearchQueryState.edit { replace(0, length, "") }
    }

    fun onServiceSelected(service: String) {
        setService(service)
        dismissServiceSelectionSheet()
    }

    fun addReceiverById(
        receiverId: Long,
        name: String,
        relation: String,
    ) {
        addReceiverIfAbsent(receiverId, name, relation)
    }

    fun addEditorMessage() {
        editorMessages.add(LeaveMessageEditorItem())
    }

    fun registerEditorMessage(message: LeaveMessageEditorItem) {
        message.tryRegister()
    }

    fun removeEditorMessage(message: LeaveMessageEditorItem) {
        editorMessages.removeAll { it.id == message.id }
    }

    /** 저장 요청에 사용할 일반 값 목록을 현재 입력 상태에서 만든다. */
    fun currentEditorMessageBlocks(): List<EditorMessageTextBlock> = editorMessages.toTextBlocks()

    /** 프리필 값으로 남기실 말씀 목록을 교체한다. */
    internal fun replaceEditorMessages(blocks: List<EditorMessageTextBlock>) {
        editorMessages.clear()
        editorMessages.addAll(blocks.toLeaveMessageEditorItems())
    }

    /** 프리필을 폼에 적용하고 계정 정보와 남기실 말씀 입력 상태를 동기화한다. */
    fun applyFormPrefill(prefill: EditorFormPrefill) {
        Log.d(
            TAG,
            "applyFormPrefill: type=${prefill.type}",
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
        idState.edit { replace(0, length, credentials?.id.orEmpty()) }
        passwordState.edit { replace(0, length, credentials?.password.orEmpty()) }
        replaceEditorMessages(prefill.leaveMessageBlocks)
    }
}

private fun List<LeaveMessageEditorItem>.toTextBlocks(): List<EditorMessageTextBlock> =
    map { m ->
        EditorMessageTextBlock(
            title = m.titleState.text.toString(),
            body = m.contentState.text.toString(),
            isRegistered = m.isRegistered,
        )
    }

private fun List<EditorMessageTextBlock>.toLeaveMessageEditorItems(): List<LeaveMessageEditorItem> =
    map { block ->
        createLeaveMessageEditorItem(
            title = block.title,
            body = block.body,
            isRegistered = block.isRegistered,
        )
    }

private fun createLeaveMessageEditorItem(
    title: String,
    body: String,
    isRegistered: Boolean,
): LeaveMessageEditorItem =
    LeaveMessageEditorItem(
        titleState = TextFieldState(title),
        contentState = TextFieldState(body),
        initialState =
            if (isRegistered) {
                LeaveMessageEditorItemState.REGISTERED_COLLAPSED
            } else {
                LeaveMessageEditorItemState.EDITING
            },
    )

private const val SAVED_EDITOR_MESSAGE_PROPERTY_COUNT = 3
private const val SAVED_EDITOR_MESSAGE_REGISTERED = "registered"
private const val SAVED_EDITOR_MESSAGE_EDITING = "editing"

/**
 * 남기실 말씀의 텍스트와 등록 여부를 화면 재생성 및 프로세스 복원에 보존한다.
 * 본문 펼침 여부는 일시적인 화면 상태라 등록 항목은 접힌 상태로 복원한다.
 */
internal val editorMessagesSaver: Saver<SnapshotStateList<LeaveMessageEditorItem>, Any> =
    listSaver(
        save = { messages ->
            messages.flatMap { message ->
                listOf(
                    message.titleState.text.toString(),
                    message.contentState.text.toString(),
                    if (message.isRegistered) {
                        SAVED_EDITOR_MESSAGE_REGISTERED
                    } else {
                        SAVED_EDITOR_MESSAGE_EDITING
                    },
                )
            }
        },
        restore = { saved ->
            if (saved.size % SAVED_EDITOR_MESSAGE_PROPERTY_COUNT != 0) return@listSaver null
            val messages =
                saved.chunked(SAVED_EDITOR_MESSAGE_PROPERTY_COUNT).map { values ->
                    val isRegistered =
                        when (values[2]) {
                            SAVED_EDITOR_MESSAGE_REGISTERED -> true
                            SAVED_EDITOR_MESSAGE_EDITING -> false
                            else -> return@listSaver null
                        }
                    createLeaveMessageEditorItem(
                        title = values[0],
                        body = values[1],
                        isRegistered = isRegistered,
                    )
                }
            mutableStateListOf<LeaveMessageEditorItem>().apply {
                addAll(messages)
            }
        },
    )

/** ViewModel의 폼과 Compose 입력 상태를 연결하는 [AfternoteEditorState]를 생성한다. */
@Composable
fun rememberAfternoteEditorState(
    getCurrentForm: () -> EditorFormState,
    setType: (AfternoteType) -> Unit,
    setService: (String) -> Unit,
    setMemorialPhoto: (String) -> Unit,
    removeMemorialPhoto: () -> Unit,
    setMemorialVideo: (String) -> Unit,
    removeMemorialVideo: () -> Unit,
    addReceiverIfAbsent: (receiverId: Long, name: String, label: String) -> Unit,
    applyPrefill: (EditorFormPrefill) -> Unit,
    setMemorialThumbnail: (String) -> Unit,
    deleteReceiver: (receiverId: Long) -> Unit,
    replaceReceiversIfEmpty: (List<AfternoteEditorReceiver>) -> Unit,
    addProcessingMethod: (text: String) -> Unit,
    deleteProcessingMethod: (localId: Int) -> Unit,
    editProcessingMethod: (localId: Int, newText: String) -> Unit,
): AfternoteEditorState {
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val serviceSearchQueryState = rememberTextFieldState()
    val editorMessages =
        rememberSaveable(saver = editorMessagesSaver) {
            mutableStateListOf()
        }

    return remember(idState, passwordState, serviceSearchQueryState, editorMessages) {
        AfternoteEditorState(
            idState = idState,
            passwordState = passwordState,
            serviceSearchQueryState = serviceSearchQueryState,
            getCurrentForm = getCurrentForm,
            setType = setType,
            setService = setService,
            setMemorialPhoto = setMemorialPhoto,
            removeMemorialPhoto = removeMemorialPhoto,
            setMemorialVideo = setMemorialVideo,
            removeMemorialVideo = removeMemorialVideo,
            addReceiverIfAbsent = addReceiverIfAbsent,
            applyPrefill = applyPrefill,
            setMemorialThumbnail = setMemorialThumbnail,
            deleteReceiver = deleteReceiver,
            replaceReceiversIfEmpty = replaceReceiversIfEmpty,
            addProcessingMethod = addProcessingMethod,
            deleteProcessingMethod = deleteProcessingMethod,
            editProcessingMethod = editProcessingMethod,
            editorMessages = editorMessages,
        )
    }
}

/** Preview와 UI 테스트에서 사용할 로컬 폼 상태를 생성한다. */
@Composable
fun rememberAfternoteEditorState(): AfternoteEditorState {
    val previewForm = remember { mutableStateOf(EditorFormState()) }
    val mutate: ((EditorFormState) -> EditorFormState) -> Unit =
        { block -> previewForm.value = block(previewForm.value) }
    return rememberAfternoteEditorState(
        getCurrentForm = { previewForm.value },
        setType = { type -> mutate { it.withType(type) } },
        setService = { service -> mutate { it.withService(service) } },
        setMemorialPhoto = { uri -> mutate { it.withMemorialPhoto(uri) } },
        removeMemorialPhoto = { mutate { it.withMemorialPhotoRemoved() } },
        setMemorialVideo = { url -> mutate { it.withMemorialVideo(url) } },
        removeMemorialVideo = { mutate { it.withMemorialVideoRemoved() } },
        addReceiverIfAbsent = { receiverId, name, label ->
            mutate { it.withReceiverAddedIfAbsent(receiverId = receiverId, name = name, label = label) }
        },
        applyPrefill = { prefill -> mutate { it.withPrefillApplied(prefill) } },
        setMemorialThumbnail = { dataUrl -> mutate { it.withMemorialThumbnail(dataUrl) } },
        deleteReceiver = { receiverId -> mutate { it.withReceiverDeleted(receiverId) } },
        replaceReceiversIfEmpty = { receivers -> mutate { it.withReceiversReplacedIfEmpty(receivers) } },
        addProcessingMethod = { text -> mutate { it.withProcessingMethodAdded(text) } },
        deleteProcessingMethod = { localId -> mutate { it.withProcessingMethodDeleted(localId) } },
        editProcessingMethod = { localId, newText ->
            mutate { it.withProcessingMethodEdited(localId = localId, newText = newText) }
        },
    )
}
