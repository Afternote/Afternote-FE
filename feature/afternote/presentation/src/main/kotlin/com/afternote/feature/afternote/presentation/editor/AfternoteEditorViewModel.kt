package com.afternote.feature.afternote.presentation.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorErrorEvent
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.withMemorialPhoto
import com.afternote.feature.afternote.presentation.editor.state.withMemorialPhotoRemoved
import com.afternote.feature.afternote.presentation.editor.state.withMemorialPlaylistSongs
import com.afternote.feature.afternote.presentation.editor.state.withMemorialThumbnail
import com.afternote.feature.afternote.presentation.editor.state.withMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.withMemorialVideoRemoved
import com.afternote.feature.afternote.presentation.editor.state.withPrefillApplied
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodAdded
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodDeleted
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodEdited
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodsInitialized
import com.afternote.feature.afternote.presentation.editor.state.withReceiverAddedIfAbsent
import com.afternote.feature.afternote.presentation.editor.state.withReceiverDeleted
import com.afternote.feature.afternote.presentation.editor.state.withReceiversReplaced
import com.afternote.feature.afternote.presentation.editor.state.withReceiversReplacedIfEmpty
import com.afternote.feature.afternote.presentation.editor.state.withService
import com.afternote.feature.afternote.presentation.editor.state.withType
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

private const val EDITOR_FORM_SNAPSHOT_KEY = "editor_form_snapshot_v4"
private const val INITIALIZED_ACTION_TEMPLATE_TYPE_KEY = "initialized_action_template_type"

private const val TAG = "AfternoteEditorViewModel"

@Serializable
private data class ReceiverSnap(
    val id: Long,
    val name: String,
    val label: String,
)

@Serializable
private data class ProcessingMethodSnap(
    val localId: Int,
    val text: String,
)

/**
 * [SavedStateHandle]에 JSON으로 넣는 폼 스냅샷. 번들 전체 크기는 대략 500KB~1MB를 넘기지 않도록 설계해야 하며,
 * 그렇지 않으면 [android.os.TransactionTooLargeException]이 날 수 있다. 큰 Base64/data URL은 폼에 넣지 말고 URL·URI 문자열만 저장한다.
 */
@Serializable
private data class EditorFormSnapshot(
    val type: AfternoteType = AfternoteType.SOCIAL_NETWORK,
    val selectedService: String = "",
    val receivers: List<ReceiverSnap> = emptyList(),
    val processingMethods: List<ProcessingMethodSnap> = emptyList(),
    val pickedMemorialPhotoUri: String? = null,
    val memorialVideo: EditableMemorialVideo? = null,
    val memorialPhotoUrl: String? = null,
    val memorialPlaylistSongs: List<Song> = emptyList(),
) {
    fun toEditorFormState(): EditorFormState =
        EditorFormState(
            afternoteEditReceivers =
                receivers.map { AfternoteEditorReceiver(id = it.id, name = it.name, label = it.label) },
            typeForm = toTypeForm(type),
        )

    /** 다른 카테고리 칸에 값이 남아 있어도 복원 단계에서 버려진다. */
    private fun toTypeForm(type: AfternoteType): AfternoteTypeForm {
        // 스냅샷의 빈 문자열은 미선택(null)로 복원 — process death 후에도 임의 기본값을 확정하지 않는다 (이슈 #468).
        val service = selectedService.ifBlank { null }
        val methodItems = processingMethods.map { ProcessingMethodItem(it.localId, it.text) }
        return when (type) {
            AfternoteType.SOCIAL_NETWORK -> {
                AfternoteTypeForm.Social(service, methodItems)
            }

            AfternoteType.BUSINESS -> {
                AfternoteTypeForm.Business(service, methodItems)
            }

            AfternoteType.GALLERY_AND_FILES -> {
                AfternoteTypeForm.Gallery(service, methodItems)
            }

            AfternoteType.MEMORIAL -> {
                AfternoteTypeForm.Memorial(
                    pickedPhotoUri = pickedMemorialPhotoUri,
                    video = memorialVideo ?: EditableMemorialVideo.empty(),
                    photoUrl = memorialPhotoUrl,
                    playlistSongs = memorialPlaylistSongs,
                )
            }

            AfternoteType.ESTATE -> {
                AfternoteTypeForm.Estate
            }
        }
    }

    companion object {
        fun from(form: EditorFormState): EditorFormSnapshot =
            EditorFormSnapshot(
                type = form.selectedType,
                selectedService = form.selectedService.orEmpty(),
                receivers =
                    form.afternoteEditReceivers.map {
                        ReceiverSnap(id = it.id, name = it.name, label = it.label)
                    },
                processingMethods = form.processingMethods.map { ProcessingMethodSnap(it.localId, it.text) },
                pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                memorialVideo = form.memorialVideo,
                memorialPhotoUrl = form.memorialPhotoUrl,
                memorialPlaylistSongs = form.memorialPlaylistSongs,
            )
    }
}

/**
 * 애프터노트 생성/수정 ViewModel.
 *
 * **SSOT:** 일반 폼은 [internalState]의 [EditorFormState], Compose 텍스트 입력은
 * [com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState]가 소유한다.
 * 추억 플레이리스트 화면과 곡 추가 화면은 같은 flow-scoped ViewModel의 폼을 사용한다.
 *
 * **경계:** Compose UI 객체(`TextFieldState`·`SnapshotStateList`·파사드)를 들지 않고 Retrofit 타입도 알지 않는다 —
 * 저장 API 의 HTTP·에러 바디 해석은 [AfternoteRepository] 구현이 도메인 예외로 변환한다.
 */
@HiltViewModel
class AfternoteEditorViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val userReceiverRepository: UserReceiverRepository,
        private val afternoteRepository: AfternoteRepository,
        private val memorialThumbnailUploadRepository: MemorialThumbnailUploadRepository,
        private val resolveMemorialMediaForSave: ResolveMemorialMediaForSaveUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<AfternoteRoute.EditorFlowRoute>()

        /** 진행 중인 prefill 조회 — 재시도가 이전 조회를 자르기 위한 핸들. */
        private var prefillJob: Job? = null

        private val formSnapshotJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        private val internalState =
            MutableStateFlow(
                InternalState(
                    form = readFormSnapshotOrDefault(),
                    originalType = route.initialType.takeIf { route.itemId != null },
                    isPrefillLoading = readEditItemId() != null,
                ),
            )

        val uiState: StateFlow<AfternoteEditorUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = AfternoteEditorUiState(form = internalState.value.form),
                )

        /** 파사드/페이로드 빌더 등이 콜백 시점에 최신 폼 스냅샷을 읽기 위한 접근자. */
        fun currentForm(): EditorFormState = internalState.value.form

        val isEditing: Boolean get() = route.itemId != null

        /**
         * 폼 SSOT 갱신의 유일한 통로. SavedState 스냅샷 직렬화도 함께 수행한다.
         * 어떤 필드를 어떻게 바꿀지는 `EditorFormMutations.kt` 의 변환 규칙이 정한다.
         */
        private fun mutateForm(block: (EditorFormState) -> EditorFormState) {
            internalState.update { prev -> prev.copy(form = block(prev.form)) }
            persistFormSnapshot(internalState.value.form)
        }

        fun setType(type: AfternoteType) {
            if (currentForm().selectedType != type) {
                savedStateHandle.remove<String>(INITIALIZED_ACTION_TEMPLATE_TYPE_KEY)
            }
            mutateForm { it.withType(type) }
        }

        fun setService(service: String) = mutateForm { it.withService(service) }

        fun setMemorialPhoto(uri: String) = mutateForm { it.withMemorialPhoto(uri) }

        /** 시트의 사진 삭제 항목. 슬롯을 비운다 — 서버 삭제는 저장 시 명시적 `null` 로 나간다(#1597, #1717). */
        fun removeMemorialPhoto() = mutateForm { it.withMemorialPhotoRemoved() }

        fun setMemorialVideo(url: String) {
            // 영상이 갈리면 이전 영상의 썸네일 실패도 함께 무효다 — 남은 바이트로 재시도하면 다른
            // 영상의 그림이 붙는다.
            pendingThumbnailBytes = null
            mutateForm { it.withMemorialVideo(url) }
        }

        /** 시트의 영상 삭제 항목. 표시된 영상이 사라지므로 그 영상의 썸네일 재시도 바이트도 함께 버린다. */
        fun removeMemorialVideo() {
            pendingThumbnailBytes = null
            mutateForm { it.withMemorialVideoRemoved() }
        }

        fun setMemorialThumbnail(dataUrl: String) = mutateForm { it.withMemorialThumbnail(dataUrl) }

        fun addMemorialPlaylistSongs(songs: List<Song>) {
            if (songs.isEmpty()) return
            mutateForm { form ->
                form.withMemorialPlaylistSongs(form.memorialPlaylistSongs + songs)
            }
        }

        fun removeMemorialPlaylistSongs(selectionKeys: Set<String>) {
            if (selectionKeys.isEmpty()) return
            mutateForm { form ->
                form.withMemorialPlaylistSongs(
                    form.memorialPlaylistSongs.filterNot { it.selectionKey in selectionKeys },
                )
            }
        }

        fun clearMemorialPlaylistSongs() {
            mutateForm { it.withMemorialPlaylistSongs(emptyList()) }
        }

        fun deleteReceiver(receiverId: Long) = mutateForm { it.withReceiverDeleted(receiverId) }

        fun addReceiverIfAbsent(
            receiverId: Long,
            name: String,
            label: String,
        ) = mutateForm { it.withReceiverAddedIfAbsent(receiverId = receiverId, name = name, label = label) }

        fun replaceReceiversIfEmpty(receivers: List<AfternoteEditorReceiver>) = mutateForm { it.withReceiversReplacedIfEmpty(receivers) }

        fun applyPrefill(prefill: EditorFormPrefill) = mutateForm { it.withPrefillApplied(prefill) }

        /**
         * 신규 작성 화면의 카테고리 추천 처리 방법을 최초 한 번만 채운다.
         *
         * 초기화 표식을 폼 스냅샷과 같은 [SavedStateHandle]에 남겨, 사용자가 추천을 전부 지운 뒤
         * 재구성·프로세스 복원이 일어나도 다시 삽입하지 않는다. 카테고리를 실제로 바꾸면
         * [setType]이 표식을 지워 새 카테고리의 추천을 받을 수 있게 한다.
         */
        fun initializeProcessingMethodDefaults(
            type: AfternoteType,
            methods: List<String>,
        ) {
            if (isEditing || currentForm().selectedType != type) return
            if (savedStateHandle.get<String>(INITIALIZED_ACTION_TEMPLATE_TYPE_KEY) == type.name) return

            savedStateHandle[INITIALIZED_ACTION_TEMPLATE_TYPE_KEY] = type.name
            if (methods.isEmpty() || currentForm().processingMethods.isNotEmpty()) return
            mutateForm { it.withProcessingMethodsInitialized(methods) }
        }

        fun addProcessingMethod(text: String) = mutateForm { it.withProcessingMethodAdded(text) }

        fun deleteProcessingMethod(localId: Int) = mutateForm { it.withProcessingMethodDeleted(localId) }

        fun editProcessingMethod(
            localId: Int,
            newText: String,
        ) = mutateForm { it.withProcessingMethodEdited(localId = localId, newText = newText) }

        init {
            readEditItemId()?.let(::loadExistingAfternoteForEdit)
        }

        private fun readEditItemId(): Long? = route.itemId

        private fun readFormSnapshotOrDefault(): EditorFormState {
            val defaultForm = EditorFormState().withType(route.initialType)
            val raw = savedStateHandle.get<String>(EDITOR_FORM_SNAPSHOT_KEY) ?: return defaultForm
            return runCatching {
                formSnapshotJson
                    .decodeFromString(EditorFormSnapshot.serializer(), raw)
                    .toEditorFormState()
            }.getOrElse { defaultForm }
        }

        /** [EditorFormSnapshot] 직렬화. 실패 시 무시한다(용량 초과 등은 [EditorFormSnapshot] KDoc 참고). */
        private fun persistFormSnapshot(form: EditorFormState) {
            runCatching {
                savedStateHandle[EDITOR_FORM_SNAPSHOT_KEY] =
                    formSnapshotJson.encodeToString(EditorFormSnapshot.serializer(), EditorFormSnapshot.from(form))
            }
        }

        /**
         * 작성자가 등록한 수신자 전체를 받아 [InternalState.authorReceivers] 에 채운다.
         *
         * 신규 작성 진입 시 1회 호출된다. 폼이 비어 있으면 화면이 이 목록으로 수신자를 채우고
         * (`AfternoteNavGraphEditor` 의 `replaceReceiversIfEmpty`), 사용자는 불필요한 수신자를 지운다.
         * 수정 진입은 상세 응답 prefill 이 지정 수신자를 채우므로 이 목록을 쓰지 않는다.
         */
        fun refreshAuthorReceivers() {
            viewModelScope.launch { loadAuthorReceivers() }
        }

        private suspend fun loadAuthorReceivers() {
            runCatchingCancellable { userReceiverRepository.getReceivers() }
                .onSuccess { receivers ->
                    internalState.update { it.copy(authorReceivers = receivers.toAfternoteEditorReceivers()) }
                }.onFailure { e ->
                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.AUTHOR_RECEIVER_LOAD, e)
                }
        }

        /**
         * 업로드에 실패한 프레임 바이트. 스낵바의 «다시 시도» 와 저장 직전 재업로드가 이것을 다시 올린다.
         *
         * 폼·UI 상태에 싣지 않는다 — 프로세스 재생성 번들에 수백 KB 를 얹지 않기 위해서다. 복원 뒤에는
         * 화면이 같은 영상에서 프레임을 다시 뽑아 [uploadMemorialThumbnail] 로 들어온다.
         */
        private var pendingThumbnailBytes: ByteArray? = null

        fun uploadMemorialThumbnail(jpegBytes: ByteArray?) {
            if (jpegBytes == null) return
            pendingThumbnailBytes = jpegBytes
            viewModelScope.launch {
                memorialThumbnailUploadRepository
                    .uploadThumbnail(jpegBytes)
                    .onSuccess { url ->
                        Log.d(TAG, "uploadMemorialThumbnail: success, url=$url")
                        pendingThumbnailBytes = null
                        internalState.update { it.copy(pendingThumbnailUrl = url) }
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_UPLOAD, e)
                        internalState.update {
                            it.withError(AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.THUMBNAIL))
                        }
                    }
            }
        }

        /**
         * 선택한 영상에서 썸네일 프레임을 뽑지 못한 실패.
         *
         * 로컬 디코딩이라 [uploadMemorialThumbnail] 경로를 타지 않는다. 종전에는 기록만 하고 화면에는
         * 아무 신호도 없어, 사용자는 썸네일 자리가 빈 이유도 되돌릴 방법도 알 수 없었다.
         */
        fun onMemorialThumbnailExtractionFailed(throwable: Throwable) {
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_EXTRACT, throwable)
            // 뽑지 못했으니 재업로드할 바이트가 없다 — 재시도는 추출부터 다시 돌아야 한다.
            pendingThumbnailBytes = null
            internalState.update {
                it.withError(AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.THUMBNAIL_EXTRACT))
            }
        }

        /**
         * 영상 재선택 없이 썸네일만 다시 만든다 (#1550).
         *
         * 손에 바이트가 있으면(업로드 실패) 그대로 다시 올리고, 없으면(추출 실패) 토큰을 올려 화면이
         * 프레임 추출부터 다시 돌게 한다. 어느 쪽이든 사용자가 고른 영상은 그대로 둔다.
         */
        fun retryMemorialThumbnail() {
            val bytes = pendingThumbnailBytes
            if (bytes != null) {
                uploadMemorialThumbnail(bytes)
                return
            }
            internalState.update { it.copy(memorialThumbnailRetryToken = it.memorialThumbnailRetryToken + 1) }
        }

        /**
         * 저장 직전, 업로드에 실패해 남아 있는 썸네일 바이트를 한 번 더 업로드 시도한다.
         *
         * 영상 자체는 저장 시점에 업로드하는데(`resolveMemorialMediaForSave`) 썸네일만 선택 시점
         * 업로드라 비대칭이었다. 실패해도 저장은 막지 않는다 — 썸네일 때문에 장례식 영상 저장을
         * 버리게 하는 편이 더 나쁘다.
         */
        private suspend fun recoverPendingThumbnailOrNull(): String? {
            val bytes = pendingThumbnailBytes ?: return null
            return memorialThumbnailUploadRepository
                .uploadThumbnail(bytes)
                .onSuccess { pendingThumbnailBytes = null }
                .onFailure { e ->
                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_UPLOAD, e)
                }.getOrNull()
        }

        /**
         * 즉석 촬영 인텐트를 띄우지 못한 실패를 기록한다.
         *
         * 화면에는 "카메라를 사용할 수 없습니다" 한 줄만 나가고 사유가 지워지므로, UI 가 예외를
         * 넘겨주지 않으면 저장공간 문제인지 카메라 앱 부재인지 콘솔에서 가를 수 없다.
         */
        fun onMemorialCaptureLaunchFailed(throwable: Throwable) {
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_CAPTURE_LAUNCH, throwable)
        }

        internal fun saveAfternote(
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            val editorState = internalState.value
            if (editorState.isSaving) return
            // prefill 을 못 읽은 채로 저장하면 서버가 빈 폼 값으로 기존 기록을 덮는다 (#705).
            // 화면이 이미 저장 액션을 막지만, 저장 진입점은 여기 하나뿐이라 규칙도 여기서 지킨다.
            //
            // 실패(isPrefillFailed)뿐 아니라 «아직 읽는 중»(isPrefillLoading)도 막는다 — skeleton 이
            // 떠 있는 동안에도 등록 버튼은 눌리고, 그때 폼은 아직 기본 빈 값이라 느린 상세 GET 을
            // 앞질러 저장하면 같은 덮어쓰기가 난다. isPrefillLoading 은 편집 진입(itemId != null)에서만
            // true 라 신규 작성은 영향받지 않는다.
            //
            // 두 상태는 저장을 막는 이유가 같아도 사용자에게 할 말이 다르다. 실패는 「불러오지
            // 못했다」 이고 진행 중은 「곧 도착한다」 다 — 한 갈래로 뭉치면 아직 읽는 중인
            // 사용자에게 실패했다고 말하게 된다.
            if (editorState.isPrefillFailed) {
                internalState.update { it.withError(AfternoteEditorError.PrefillUnavailable) }
                return
            }
            if (editorState.isPrefillLoading) {
                internalState.update { it.withError(AfternoteEditorError.PrefillNotReady) }
                return
            }

            val form = editorState.form
            val editingId = readEditItemId()
            val type = form.selectedType
            val playlistSongs = form.memorialPlaylistSongs

            val validationError =
                AfternoteEditorValidator.validate(
                    form = form,
                    payload = payload,
                )
            if (validationError != null) {
                internalState.update {
                    it.withError(AfternoteEditorError.Validation(validationError))
                }
                return
            }

            val typeForSave =
                if (editingId != null) (editorState.originalType ?: type) else type

            viewModelScope.launch {
                internalState.update {
                    it.copy(isSaving = true, errorEvent = null)
                }
                // 썸네일 URL 이 비어 있고 업로드에 실패한 바이트가 남아 있으면, payload 를 만들기 전에 그
                // 바이트를 한 번 더 업로드해 본다. 여기서 놓치면 썸네일 없는 영상이 그대로 확정되고,
                // 재편집으로 들어와도 원격 URL 이라 프레임을 다시 뽑지 않는다 (#1550). 실패해도 저장은
                // 그대로 진행한다.
                val memorialMediaForSave =
                    if (memorialMedia.memorialVideo.displayed?.thumbnailUrl != null) {
                        memorialMedia
                    } else {
                        recoverPendingThumbnailOrNull()
                            ?.let { url -> memorialMedia.copy(memorialVideo = memorialMedia.memorialVideo.withSelectionThumbnail(url)) }
                            ?: memorialMedia
                    }
                buildSaveCommand(
                    editingId = editingId,
                    typeForSave = typeForSave,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
                    memorialMedia = memorialMediaForSave,
                ).fold(
                    onSuccess = { command ->
                        executeSaveCommand(command).fold(
                            onSuccess = { id ->
                                internalState.update {
                                    it.copy(
                                        isSaving = false,
                                        savedId = id,
                                        pendingSaveSuccessId = id,
                                    )
                                }
                            },
                            onFailure = { e -> handleSaveFailure(e) },
                        )
                    },
                    onFailure = { e -> handleSaveFailure(e) },
                )
            }
        }

        /**
         * [SaveAfternoteCommand] 분기에 따라 [AfternoteRepository] 의 적합한 메서드를 직접 호출한다.
         *
         * 과거에는 별도 `SaveAfternoteUseCase` 로 분리돼 있었으나, 단일 Repository 내 메서드 라우팅
         * 외에 비즈니스 로직이 없어 *약한 UseCase* (`#246`) 로 판단해 ViewModel 로 흡수.
         */
        private suspend fun executeSaveCommand(command: SaveAfternoteCommand): Result<Long> =
            when (command) {
                is SaveAfternoteCommand.Create -> {
                    when (val input = command.input) {
                        is CreateAfternoteInput.Social -> afternoteRepository.createSocial(input.payload)
                        is CreateAfternoteInput.Business -> afternoteRepository.createBusiness(input.payload)
                        is CreateAfternoteInput.Gallery -> afternoteRepository.createGallery(input.payload)
                        is CreateAfternoteInput.Memorial -> afternoteRepository.createMemorial(input.payload)
                    }
                }

                is SaveAfternoteCommand.Update -> {
                    afternoteRepository.update(command.id, command.payload)
                }
            }

        // 영정 사진: 새로 고른 로컬 픽 우선 → 없으면 기존 원격 → 둘 다 없으면 없음.
        private fun photoMediaInput(
            picked: String?,
            existing: String?,
        ): MediaInput =
            when {
                !picked.isNullOrBlank() -> MediaInput.Local(picked)
                !existing.isNullOrBlank() -> MediaInput.Remote(existing)
                else -> MediaInput.None
            }

        private suspend fun buildSaveCommand(
            editingId: Long?,
            typeForSave: AfternoteType,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistSongs: List<Song>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ): Result<SaveAfternoteCommand> {
            val resolved =
                resolveMemorialMediaForSave(
                    video = memorialMedia.memorialVideo.toMediaInput(),
                    photo =
                        photoMediaInput(
                            picked = memorialMedia.pickedMemorialPhotoUri,
                            existing = memorialMedia.memorialPhotoUrl,
                        ),
                ).getOrElse { return Result.failure(it) }

            val command =
                if (editingId != null) {
                    val updatePayload =
                        AfternoteEditorFormMapper.buildUpdatePayload(
                            type = typeForSave,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            memorialMedia =
                                MemorialMediaUrls(
                                    memorialVideoUrl = resolved.resolvedVideoUrl,
                                    memorialThumbnailUrl = memorialMedia.memorialVideo.displayed?.thumbnailUrl,
                                    memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                                ),
                        )
                    SaveAfternoteCommand.Update(id = editingId, payload = updatePayload)
                } else {
                    val createInput =
                        AfternoteEditorFormMapper.buildCreateInput(
                            type = typeForSave,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            memorialVideoUrl = resolved.resolvedVideoUrl,
                            memorialThumbnailUrl = memorialMedia.memorialVideo.displayed?.thumbnailUrl,
                            memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                        )
                    SaveAfternoteCommand.Create(input = createInput)
                }
            return Result.success(command)
        }

        /**
         * 수정 진입 prefill 조회 실패 화면의 «다시 시도» (#705).
         *
         * 실패 상태를 걷고 skeleton 을 다시 세운 뒤 같은 조회를 새로 건다. 이미 성공해 폼이 채워진
         * 뒤라면 부를 일이 없고(화면이 오류 상태에서만 버튼을 그린다), 신규 작성 진입은 [readEditItemId]
         * 가 null 이라 아무 일도 하지 않는다.
         */
        fun retryPrefill() {
            val afternoteId = readEditItemId() ?: return
            loadExistingAfternoteForEdit(afternoteId)
        }

        /**
         * 수정 진입 시 기존 애프터노트를 읽어 폼에 실을 prefill 을 만든다.
         *
         * 실패를 «빈 폼» 으로 흘려보내지 않는다 (#705) — 서버 수정(PATCH)은 보낸 값으로 기존 기록을
         * 덮으므로, 못 읽은 상태의 빈 폼이 저장되면 기록이 소실된다. 그래서 실패는 [InternalState.isPrefillFailed]
         * 로 남겨 화면이 오류·재시도를 그리고 [saveAfternote] 가 저장을 막게 한다.
         */
        private fun loadExistingAfternoteForEdit(afternoteId: Long) {
            // 재시도가 진행 중인 조회를 자르고 들어온다 — 자르지 않으면 두 응답이 같은 폼을 두고 경합한다.
            prefillJob?.cancel()
            prefillJob =
                viewModelScope.launch {
                    internalState.update { it.copy(isPrefillLoading = true, isPrefillFailed = false) }
                    afternoteRepository
                        .getDetail(id = afternoteId)
                        .onSuccess { detail ->
                            val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(detail)
                            // UI 레이어 파사드가 TextFieldState·SnapshotStateList 등 UI 상태를 갱신하도록 위임.
                            // skeleton 종료는 UI 가 prefill 적용을 마친 뒤 [onPrefillConsumed] 로 통보한다
                            // (uiState 갱신 시점에 prefill 도착했어도 UI 가 form·TextFieldState 에 반영하기 전이라
                            //  여기서 끄면 skeleton 사라짐 → 빈 폼 → prefill 깜빡임 발생).
                            internalState.update {
                                it.copy(
                                    originalType = prefill.type,
                                    pendingPrefill = prefill,
                                )
                            }
                        }.onFailure { e ->
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.PREFILL_LOAD, e)
                            // skeleton 은 걷되 빈 폼으로 넘기지 않는다 — 오류·재시도 상태로 남긴다.
                            internalState.update { it.copy(isPrefillLoading = false, isPrefillFailed = true) }
                        }
                }
        }

        /**
         * UI 가 [AfternoteEditorUiState.pendingPrefill] 신호를 받아 폼·TextFieldState 에 모두 반영한 직후 호출.
         * skeleton 종료([AfternoteEditorUiState.isPrefillLoading] = false) + 신호 reset (pendingPrefill = null) 동시 처리.
         */
        fun onPrefillConsumed() {
            internalState.update { it.copy(isPrefillLoading = false, pendingPrefill = null) }
        }

        private fun handleSaveFailure(e: Throwable) {
            val editorError = e.toAfternoteEditorError()
            // 필수 필드 검증에 걸린 실패(수신자 미선택 등)는 사용자가 채우면 풀리는 정상 경로라 기록하지 않는다 —
            // 보관 한도(최근 8건) 를 사용자 오류가 차지하면 실제 등록 장애가 밀려난다.
            // 여기 걸리는 건 검증 외 실패 전부다 — 그중 5xx 본문엔 내부 SQL 이 섞여 오므로 예외 타입만 남긴다.
            if (editorError !is AfternoteEditorError.Validation) {
                errorReporter.recordAfternoteFailure(AfternoteFailureStage.SAVE, e)
            }
            internalState.update {
                it.copy(isSaving = false).withError(editorError)
            }
        }

        /**
         * 수신자 선택 화면이 돌려준 id 를 폼에 넣을 수 있는 값으로 해석한다.
         *
         * [refreshAuthorReceivers] 로 받아 둔 목록에 없으면 — 그 로드가 실패했다는 뜻이므로 — 한 번 더 받아 보고,
         * 그래도 못 찾으면 [AfternoteEditorError.ReceiverSelectionUnavailable] 을 세워 화면이 알리게 한다.
         * 이 신호가 없으면 사용자가 고른 수신자가 아무 표시 없이 사라진다 (#1405).
         */
        suspend fun resolveSelectedReceiver(id: Long): AfternoteEditorReceiver? {
            findReceiverById(id)?.let { return it }
            loadAuthorReceivers()
            return findReceiverById(id) ?: run {
                internalState.update { it.withError(AfternoteEditorError.ReceiverSelectionUnavailable) }
                null
            }
        }

        /**
         * 수신자 선택 화면이 확정한 [receiverIds] 전체를 폼에 반영한다 (#1426).
         *
         * 화면은 폼의 현재 수신자를 선택 상태로 열고 확정된 전체를 돌려준다 — 그래서 반영은
         * «추가» 가 아니라 «교체» 다. 화면에서 푼 수신자는 폼에서도 빠진다.
         *
         * 이미 폼에 있는 id 는 표시에 필요한 이름·관계를 폼이 이미 들고 있으므로 재조회하지 않는다.
         * 새로 들어온 id 만 [resolveSelectedReceiver] 로 해석하고, 해석 실패는 그쪽이 오류 이벤트로
         * 알린다 — 그 id 만 빠지고 나머지 선택은 반영된다 (#1405).
         */
        suspend fun applySelectedReceivers(receiverIds: List<Long>) {
            val alreadyInForm = currentForm().afternoteEditReceivers.associateBy { it.id }
            val next = receiverIds.mapNotNull { id -> alreadyInForm[id] ?: resolveSelectedReceiver(id) }
            mutateForm { it.withReceiversReplaced(next) }
        }

        private fun findReceiverById(id: Long): AfternoteEditorReceiver? = internalState.value.authorReceivers.find { it.id == id }

        // region Internal state shaping

        /**
         * VM 내부에서만 다루는 평탄한 상태.
         * public [AfternoteEditorUiState] 는 이 값을 [toUiState] 로 매핑해 노출한다.
         */
        private data class InternalState(
            val form: EditorFormState = EditorFormState(),
            val originalType: AfternoteType? = null,
            val authorReceivers: List<AfternoteEditorReceiver> = emptyList(),
            val isSaving: Boolean = false,
            val isPrefillLoading: Boolean = false,
            val isPrefillFailed: Boolean = false,
            val savedId: Long? = null,
            val errorEvent: AfternoteEditorErrorEvent? = null,
            val errorOccurrence: Long = 0L,
            val pendingSaveSuccessId: Long? = null,
            val pendingThumbnailUrl: String? = null,
            val memorialThumbnailRetryToken: Int = 0,
            val pendingPrefill: EditorFormPrefill? = null,
        )

        private fun InternalState.toUiState(): AfternoteEditorUiState =
            AfternoteEditorUiState(
                form = form,
                authorReceivers = authorReceivers,
                isSaving = isSaving,
                isPrefillLoading = isPrefillLoading,
                isPrefillFailed = isPrefillFailed,
                savedId = savedId,
                errorEvent = errorEvent,
                pendingSaveSuccessId = pendingSaveSuccessId,
                pendingThumbnailUrl = pendingThumbnailUrl,
                memorialThumbnailRetryToken = memorialThumbnailRetryToken,
                pendingPrefill = pendingPrefill,
            )

        fun onSaveSuccessConsumed() {
            internalState.update { it.copy(pendingSaveSuccessId = null) }
        }

        fun onThumbnailUploadedConsumed() {
            internalState.update { it.copy(pendingThumbnailUrl = null) }
        }

        private fun InternalState.withError(error: AfternoteEditorError): InternalState {
            val nextOccurrence = errorOccurrence + 1L
            return copy(
                errorEvent = AfternoteEditorErrorEvent(error, nextOccurrence),
                errorOccurrence = nextOccurrence,
            )
        }

        fun onErrorConsumed(consumed: AfternoteEditorErrorEvent) {
            internalState.update { current ->
                if (current.errorEvent == consumed) current.copy(errorEvent = null) else current
            }
        }

        // endregion
    }

/**
 * 저장 실패를 화면이 소비할 단일 오류 상태로 좁힌다.
 *
 * [AfternoteFailure] 는 루트로 받아 `when` 을 exhaustive 하게 만든다 — 실패 유형이 늘면 여기가
 * 컴파일 에러로 잡힌다. `else` 로 뭉개 두면 새 유형이 조용히 서버 오류로 흘러간다.
 */
internal fun Throwable.toAfternoteEditorError(): AfternoteEditorError =
    when (this) {
        is AfternoteFailure -> {
            when (this) {
                // 미디어 해석 실패는 사용자가 입력을 고쳐 푸는 검증 실패가 아니라 업로드 장애다.
                is AfternoteFailure.MediaSave -> {
                    AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA)
                }

                is AfternoteFailure.NetworkUnavailable -> {
                    AfternoteEditorError.Network
                }
            }
        }

        else -> {
            AfternoteEditorError.Server
        }
    }
