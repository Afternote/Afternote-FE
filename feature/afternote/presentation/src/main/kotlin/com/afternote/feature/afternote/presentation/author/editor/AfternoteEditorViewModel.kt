package com.afternote.feature.afternote.presentation.author.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.AuthorReceiverRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.MemorialPhotoSaveException
import com.afternote.feature.afternote.domain.usecase.editor.MemorialVideoSaveException
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorSaveError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationException
import com.afternote.feature.afternote.presentation.author.editor.state.DEFAULT_EDITOR_MESSAGE_BLOCKS
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.author.editor.state.withLeaveMessageBlocks
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialPhoto
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialPlaylistSongs
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialThumbnail
import com.afternote.feature.afternote.presentation.author.editor.state.withMemorialVideo
import com.afternote.feature.afternote.presentation.author.editor.state.withPrefillApplied
import com.afternote.feature.afternote.presentation.author.editor.state.withProcessingMethodAdded
import com.afternote.feature.afternote.presentation.author.editor.state.withProcessingMethodDeleted
import com.afternote.feature.afternote.presentation.author.editor.state.withProcessingMethodEdited
import com.afternote.feature.afternote.presentation.author.editor.state.withReceiverAddedIfAbsent
import com.afternote.feature.afternote.presentation.author.editor.state.withReceiverDeleted
import com.afternote.feature.afternote.presentation.author.editor.state.withReceiversReplacedIfEmpty
import com.afternote.feature.afternote.presentation.author.editor.state.withService
import com.afternote.feature.afternote.presentation.author.editor.state.withType
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
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

private const val EDITOR_FORM_SNAPSHOT_KEY = "editor_form_snapshot_v2"

private const val TAG = "AfternoteEditorViewModel"

@Serializable
private data class ReceiverSnap(
    val id: String,
    val name: String,
    val label: String,
)

@Serializable
private data class ProcessingMethodSnap(
    val localId: Int,
    val text: String,
)

@Serializable
private data class MessageBlockSnap(
    val title: String = "",
    val body: String = "",
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
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val memorialPlaylistSongs: List<Song> = emptyList(),
    val editorMessages: List<MessageBlockSnap> = emptyList(),
) {
    fun toEditorFormState(restoreGeneration: Long): EditorFormState {
        val blocks: List<EditorMessageTextBlock> =
            if (editorMessages.isEmpty()) {
                DEFAULT_EDITOR_MESSAGE_BLOCKS
            } else {
                editorMessages.map { EditorMessageTextBlock(title = it.title, body = it.body) }
            }
        return EditorFormState(
            afternoteEditReceivers =
                receivers.map { AfternoteEditorReceiver(id = it.id, name = it.name, label = it.label) },
            leaveMessageBlocks = blocks,
            leaveMessageBlocksRestoreGeneration = restoreGeneration,
            typeForm = toTypeForm(type),
        )
    }

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
                    videoUrl = memorialVideoUrl,
                    thumbnailUrl = memorialThumbnailUrl,
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
                memorialVideoUrl = form.memorialVideoUrl,
                memorialThumbnailUrl = form.memorialThumbnailUrl,
                memorialPhotoUrl = form.memorialPhotoUrl,
                memorialPlaylistSongs = form.memorialPlaylistSongs,
                editorMessages =
                    form.leaveMessageBlocks.map { MessageBlockSnap(title = it.title, body = it.body) },
            )
    }
}

/**
 * 애프터노트 생성/수정 ViewModel.
 *
 * **SSOT:** 에디터 flow 전체의 폼은 [internalState] 안의 [EditorFormState]다.
 * 추억 플레이리스트 화면과 곡 추가 화면도 같은 flow-scoped ViewModel을 사용한다.
 *
 * **경계:** Compose UI 객체(`TextFieldState`·`SnapshotStateList`·파사드)를 들지 않고 Retrofit 타입도 알지 않는다 —
 * 저장 API 의 HTTP·에러 바디 해석은 [AfternoteRepository] 구현이 도메인 예외로 변환한다.
 */
@HiltViewModel
class AfternoteEditorViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val authorReceiverRepository: AuthorReceiverRepository,
        private val afternoteRepository: AfternoteRepository,
        private val memorialThumbnailUploadRepository: MemorialThumbnailUploadRepository,
        private val resolveMemorialMediaForSave: ResolveMemorialMediaForSaveUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val route = savedStateHandle.toRoute<AfternoteRoute.EditorFlowRoute>()
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

        fun setType(type: AfternoteType) = mutateForm { it.withType(type) }

        fun setService(service: String) = mutateForm { it.withService(service) }

        fun setMemorialPhoto(uri: String?) = mutateForm { it.withMemorialPhoto(uri) }

        fun setMemorialVideo(url: String?) = mutateForm { it.withMemorialVideo(url) }

        fun setMemorialThumbnail(dataUrl: String?) = mutateForm { it.withMemorialThumbnail(dataUrl) }

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

        fun deleteReceiver(receiverId: String) = mutateForm { it.withReceiverDeleted(receiverId) }

        fun addReceiverIfAbsent(
            receiverId: String,
            name: String,
            label: String,
        ) = mutateForm { it.withReceiverAddedIfAbsent(receiverId = receiverId, name = name, label = label) }

        fun replaceReceiversIfEmpty(receivers: List<AfternoteEditorReceiver>) = mutateForm { it.withReceiversReplacedIfEmpty(receivers) }

        fun setLeaveMessageBlocks(blocks: List<EditorMessageTextBlock>) = mutateForm { it.withLeaveMessageBlocks(blocks) }

        fun applyPrefill(prefill: EditorFormPrefill) = mutateForm { it.withPrefillApplied(prefill) }

        fun addProcessingMethod(text: String) = mutateForm { it.withProcessingMethodAdded(text) }

        fun deleteProcessingMethod(localId: Int) = mutateForm { it.withProcessingMethodDeleted(localId) }

        fun editProcessingMethod(
            localId: Int,
            newText: String,
        ) = mutateForm { it.withProcessingMethodEdited(localId = localId, newText = newText) }

        init {
            viewModelScope.launch {
                authorReceiverRepository
                    .observeReceivers()
                    .map { it.toAfternoteEditorReceivers() }
                    .collect { mapped ->
                        internalState.update { it.copy(authorReceivers = mapped) }
                    }
            }
            readEditItemId()?.let(::loadExistingAfternoteForEdit)
        }

        private fun readEditItemId(): Long? = route.itemId

        private fun readFormSnapshotOrDefault(): EditorFormState {
            val defaultForm = EditorFormState().withType(route.initialType)
            val raw = savedStateHandle.get<String>(EDITOR_FORM_SNAPSHOT_KEY) ?: return defaultForm
            return runCatching {
                formSnapshotJson
                    .decodeFromString(EditorFormSnapshot.serializer(), raw)
                    .toEditorFormState(restoreGeneration = System.nanoTime())
            }.getOrElse { defaultForm }
        }

        /** [EditorFormSnapshot] 직렬화. 실패 시 무시한다(용량 초과 등은 [EditorFormSnapshot] KDoc 참고). */
        private fun persistFormSnapshot(form: EditorFormState) {
            runCatching {
                savedStateHandle[EDITOR_FORM_SNAPSHOT_KEY] =
                    formSnapshotJson.encodeToString(EditorFormSnapshot.serializer(), EditorFormSnapshot.from(form))
            }
        }

        fun refreshAuthorReceivers() {
            viewModelScope.launch {
                authorReceiverRepository.refreshReceivers()
            }
        }

        fun uploadMemorialThumbnail(jpegBytes: ByteArray?) {
            if (jpegBytes == null) return
            viewModelScope.launch {
                memorialThumbnailUploadRepository
                    .uploadThumbnail(jpegBytes)
                    .onSuccess { url ->
                        Log.d(TAG, "uploadMemorialThumbnail: success, url=$url")
                        internalState.update { it.copy(pendingThumbnailUrl = url) }
                    }.onFailure { e ->
                        Log.e(TAG, "uploadMemorialThumbnail: failed", e)
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_UPLOAD, e)
                        internalState.update {
                            it.copy(
                                saveError =
                                    AfternoteEditorSaveError.Upload(
                                        AfternoteEditorSaveError.Upload.Target.THUMBNAIL,
                                    ),
                            )
                        }
                    }
            }
        }

        /**
         * 선택한 영상에서 썸네일 프레임을 뽑지 못한 실패를 기록한다.
         *
         * 로컬 디코딩이라 [uploadMemorialThumbnail] 경로를 타지 않고, 사용자에게도 썸네일 자리가
         * 비어 보일 뿐 오류로 알려주지 않아 UI 가 넘겨주지 않으면 콘솔에 흔적이 남지 않는다.
         */
        fun onMemorialThumbnailExtractionFailed(throwable: Throwable) {
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_EXTRACT, throwable)
        }

        fun saveAfternote(
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            val editorState = internalState.value
            if (editorState.isSaving) return

            val form = editorState.form
            val editingId = readEditItemId()
            val type = form.selectedType
            val playlistSongs = form.memorialPlaylistSongs

            val validationError =
                AfternoteEditorValidator.validate(
                    form = form,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                )
            if (validationError != null) {
                internalState.update {
                    it.copy(saveError = AfternoteEditorSaveError.Validation(validationError))
                }
                return
            }

            val typeForSave =
                if (editingId != null) (editorState.originalType ?: type) else type

            viewModelScope.launch {
                internalState.update {
                    it.copy(isSaving = true, saveError = null)
                }
                buildSaveCommand(
                    editingId = editingId,
                    typeForSave = typeForSave,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
                    memorialMedia = memorialMedia,
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

        // 영상: 로컬 pick(content://) 인지 원격 prefill URL 인지를 진입 경계에서 한 번 확정해 MediaInput 으로 넘긴다.
        private fun videoMediaInput(url: String?): MediaInput {
            if (url.isNullOrBlank()) return MediaInput.None
            return if (url.startsWith("content://")) MediaInput.Local(url) else MediaInput.Remote(url)
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
                    video = videoMediaInput(memorialMedia.memorialVideoUrl),
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
                                    memorialThumbnailUrl = memorialMedia.memorialThumbnailUrl,
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
                            memorialThumbnailUrl = memorialMedia.memorialThumbnailUrl,
                            memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                        )
                    SaveAfternoteCommand.Create(input = createInput)
                }
            return Result.success(command)
        }

        private fun loadExistingAfternoteForEdit(afternoteId: Long) {
            viewModelScope.launch {
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
                        Log.e(TAG, "loadExistingAfternoteForEdit: id=$afternoteId failed", e)
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.PREFILL_LOAD, e)
                        // 실패 시 skeleton 에 갇히지 않도록 즉시 종료.
                        internalState.update { it.copy(isPrefillLoading = false) }
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
            val saveError = e.toAfternoteEditorSaveError()
            // 필수 필드 검증에 걸린 실패(수신자 미선택 등)는 사용자가 채우면 풀리는 정상 경로라 기록하지 않는다 —
            // 보관 한도(최근 8건) 를 사용자 오류가 차지하면 실제 등록 장애가 밀려난다.
            // 여기 걸리는 건 검증 외 실패 전부다 — 그중 5xx 본문엔 내부 SQL 이 섞여 오므로 예외 타입만 남긴다.
            if (saveError !is AfternoteEditorSaveError.Validation) {
                Log.e(TAG, "handleSaveFailure: ${e.javaClass.name}")
                errorReporter.recordAfternoteFailure(AfternoteFailureStage.SAVE, e)
            }
            internalState.update {
                it.copy(
                    isSaving = false,
                    saveError = saveError,
                )
            }
        }

        fun getReceiverById(id: Long): AuthorReceiverEntry? = authorReceiverRepository.currentReceivers().find { it.receiverId == id }

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
            val savedId: Long? = null,
            val saveError: AfternoteEditorSaveError? = null,
            val pendingSaveSuccessId: Long? = null,
            val pendingThumbnailUrl: String? = null,
            val pendingPrefill: EditorFormPrefill? = null,
        )

        private fun InternalState.toUiState(): AfternoteEditorUiState =
            AfternoteEditorUiState(
                form = form,
                authorReceivers = authorReceivers,
                isSaving = isSaving,
                isPrefillLoading = isPrefillLoading,
                savedId = savedId,
                saveError = saveError,
                pendingSaveSuccessId = pendingSaveSuccessId,
                pendingThumbnailUrl = pendingThumbnailUrl,
                pendingPrefill = pendingPrefill,
            )

        fun onSaveSuccessConsumed() {
            internalState.update { it.copy(pendingSaveSuccessId = null) }
        }

        fun onThumbnailUploadedConsumed() {
            internalState.update { it.copy(pendingThumbnailUrl = null) }
        }

        fun onSaveErrorConsumed() {
            internalState.update { it.copy(saveError = null) }
        }

        // endregion
    }

internal fun Throwable.toAfternoteEditorSaveError(): AfternoteEditorSaveError =
    when (this) {
        is AfternoteAuthoringValidationException -> {
            val reason =
                when (kind) {
                    AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED -> AfternoteValidationError.RECEIVERS_REQUIRED
                }
            AfternoteEditorSaveError.Validation(reason)
        }

        is AfternoteValidationException -> {
            AfternoteEditorSaveError.Validation(validationError)
        }

        is NetworkUnavailableException -> {
            AfternoteEditorSaveError.Network
        }

        is MemorialPhotoSaveException, is MemorialVideoSaveException -> {
            AfternoteEditorSaveError.Upload(AfternoteEditorSaveError.Upload.Target.SAVE_MEDIA)
        }

        else -> {
            AfternoteEditorSaveError.Server
        }
    }
