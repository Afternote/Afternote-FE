package com.afternote.feature.afternote.presentation.author.editor.state

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
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.message.LeaveMessageEditorItem
import com.afternote.feature.afternote.presentation.author.editor.message.LeaveMessageEditorItemState
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCredentialsPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

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
    val customServiceNameState: TextFieldState,
    private val getCurrentForm: () -> EditorFormState,
    private val setType: (AfternoteType) -> Unit,
    private val setService: (String) -> Unit,
    private val addReceiverIfAbsent: (receiverId: String, name: String, label: String) -> Unit,
    private val applyPrefill: (EditorFormPrefill) -> Unit,
    val setMemorialPhoto: (String?) -> Unit,
    val setMemorialVideo: (String?) -> Unit,
    val setMemorialThumbnail: (String?) -> Unit,
    val deleteReceiver: (receiverId: String) -> Unit,
    val replaceReceiversIfEmpty: (List<AfternoteEditorReceiver>) -> Unit,
    val addProcessingMethod: (text: String) -> Unit,
    val deleteProcessingMethod: (localId: Int) -> Unit,
    val editProcessingMethod: (localId: Int, newText: String) -> Unit,
    val editorMessages: SnapshotStateList<LeaveMessageEditorItem> =
        mutableStateListOf(LeaveMessageEditorItem()),
) {
    var isCustomServiceDialogVisible by mutableStateOf(false)
        private set

    var typeDropdownExpanded by mutableStateOf(false)
        private set

    var serviceDropdownExpanded by mutableStateOf(false)
        private set

    /** 이벤트 처리에 사용할 최신 폼. 화면 표시는 수집된 `uiState.form`을 사용한다. */
    fun currentForm(): EditorFormState = getCurrentForm()

    fun onTypeDropdownExpandedChange(expanded: Boolean) {
        typeDropdownExpanded = expanded
    }

    fun onServiceDropdownExpandedChange(expanded: Boolean) {
        serviceDropdownExpanded = expanded
    }

    fun onTypeSelected(type: AfternoteType) = setType(type)

    fun onServiceSelected(service: String) {
        if (getCurrentForm().isCustomAddOption(service)) {
            isCustomServiceDialogVisible = true
        } else {
            setService(service)
        }
    }

    fun dismissCustomServiceDialog() {
        isCustomServiceDialogVisible = false
        customServiceNameState.edit { replace(0, length, "") }
    }

    fun onAddCustomService() {
        val serviceName =
            customServiceNameState.text
                .toString()
                .trim()
        if (serviceName.isEmpty()) return
        setService(serviceName)
        dismissCustomServiceDialog()
    }

    fun addReceiverById(
        receiverId: Long,
        name: String,
        relation: String,
    ) {
        addReceiverIfAbsent(receiverId.toString(), name, relation)
    }

    fun addEditorMessage() {
        editorMessages.add(LeaveMessageEditorItem())
    }

    fun registerEditorMessage(message: LeaveMessageEditorItem) {
        message.tryRegister()
    }

    fun removeEditorMessage(message: LeaveMessageEditorItem) {
        if (editorMessages.size <= 1) return
        editorMessages.removeAll { it.id == message.id }
    }

    /** 저장 요청에 사용할 일반 값 목록을 현재 입력 상태에서 만든다. */
    fun currentEditorMessageBlocks(): List<EditorMessageTextBlock> = editorMessages.toTextBlocks()

    /** 프리필처럼 화면 밖에서 받은 값으로 남기실 말씀 목록을 교체한다. */
    internal fun replaceEditorMessages(blocks: List<EditorMessageTextBlock>) {
        editorMessages.clear()
        for (b in normalized) {
            val msg =
                LeaveMessageEditorItem(
                    initialState =
                        if (b.isRegistered) {
                            LeaveMessageEditorItemState.REGISTERED_COLLAPSED
                        } else {
                            LeaveMessageEditorItemState.EDITING
                        },
                )
            msg.titleState.edit { replace(0, length, b.title) }
            msg.contentState.edit { replace(0, length, b.body) }
            editorMessages.add(msg)
        }
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

/** ViewModel의 폼과 Compose 입력 상태를 연결하는 [AfternoteEditorState]를 생성한다. */
@Composable
fun rememberAfternoteEditorState(
    getCurrentForm: () -> EditorFormState,
    setType: (AfternoteType) -> Unit,
    setService: (String) -> Unit,
    setMemorialPhoto: (String?) -> Unit,
    setMemorialVideo: (String?) -> Unit,
    addReceiverIfAbsent: (receiverId: String, name: String, label: String) -> Unit,
    applyPrefill: (EditorFormPrefill) -> Unit,
    setMemorialThumbnail: (String?) -> Unit,
    deleteReceiver: (receiverId: String) -> Unit,
    replaceReceiversIfEmpty: (List<AfternoteEditorReceiver>) -> Unit,
    addProcessingMethod: (text: String) -> Unit,
    deleteProcessingMethod: (localId: Int) -> Unit,
    editProcessingMethod: (localId: Int, newText: String) -> Unit,
): AfternoteEditorState {
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val customServiceNameState = rememberTextFieldState()

    return remember(idState, passwordState, customServiceNameState, editorMessages) {
        AfternoteEditorState(
            idState = idState,
            passwordState = passwordState,
            customServiceNameState = customServiceNameState,
            getCurrentForm = getCurrentForm,
            setType = setType,
            setService = setService,
            setMemorialPhoto = setMemorialPhoto,
            setMemorialVideo = setMemorialVideo,
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
        setMemorialVideo = { url -> mutate { it.withMemorialVideo(url) } },
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
