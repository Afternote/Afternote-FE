package com.afternote.feature.afternote.presentation.author.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.AuthorReceiverRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationException
import com.afternote.feature.afternote.presentation.author.editor.state.DEFAULT_EDITOR_MESSAGE_BLOCKS
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
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

private const val TAG = "AfternoteEditorVM"

private const val EDITOR_FORM_SNAPSHOT_KEY = "editor_form_snapshot_v1"

/** 수정 진입 시 서버 원본 카테고리(API `categoryForApi`). 폼 스냅샷과 별도로 두어 프로세스 데스 후에도 유지한다. */
private const val EDITOR_ORIGINAL_CATEGORY_FOR_API_KEY = "editor_original_category_for_api_v1"

/** 타입 안전 [com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute.EditorRoute] 직렬화 인자명 (상세 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel]과 동일). */
private const val NAV_ARG_ITEM_ID = "itemId"

@Serializable
private data class ReceiverSnap(
    val id: String,
    val name: String,
    val label: String,
)

@Serializable
private data class PmSnap(
    val id: String,
    val text: String,
)

@Serializable
private data class AlbumSnap(
    val id: String,
    val imageUrl: String? = null,
    val title: String? = null,
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
    val loadedItemId: String? = null,
    val categoryName: String = "SOCIAL",
    val selectedService: String = "",
    val receivers: List<ReceiverSnap> = emptyList(),
    val social: List<PmSnap> = emptyList(),
    val gallery: List<PmSnap> = emptyList(),
    val selectedLastWish: String? = null,
    val pickedMemorialPhotoUri: String? = null,
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val playlistSongCount: Int = 16,
    val memorialPlaylistSongs: List<Song> = emptyList(),
    val albumCovers: List<AlbumSnap> = emptyList(),
    val editorMessages: List<MessageBlockSnap> = emptyList(),
) {
    fun toEditorFormState(restoreGeneration: Long): EditorFormState {
        val category =
            runCatching { EditorCategory.valueOf(categoryName) }.getOrElse { EditorCategory.SOCIAL }
        val service =
            selectedService.ifBlank {
                if (category == EditorCategory.GALLERY) {
                    AfternoteServiceCatalog.defaultGalleryService
                } else {
                    AfternoteServiceCatalog.defaultSocialService
                }
            }
        val blocks: List<EditorMessageTextBlock> =
            if (editorMessages.isEmpty()) {
                DEFAULT_EDITOR_MESSAGE_BLOCKS
            } else {
                editorMessages.map { EditorMessageTextBlock(title = it.title, body = it.body) }
            }
        return EditorFormState(
            loadedItemId = loadedItemId,
            selectedCategory = category,
            selectedService = service,
            afternoteEditReceivers =
                receivers.map { AfternoteEditorReceiver(id = it.id, name = it.name, label = it.label) },
            socialProcessingMethods = social.map { ProcessingMethodItem(it.id, it.text) },
            galleryProcessingMethods = gallery.map { ProcessingMethodItem(it.id, it.text) },
            selectedLastWish = selectedLastWish,
            pickedMemorialPhotoUri = pickedMemorialPhotoUri,
            funeralVideoUrl = funeralVideoUrl,
            funeralThumbnailUrl = funeralThumbnailUrl,
            memorialPhotoUrl = memorialPhotoUrl,
            playlistSongCount = playlistSongCount,
            memorialPlaylistSongs = memorialPlaylistSongs,
            playlistAlbumCovers =
                albumCovers.map { AlbumCover(id = it.id, imageUrl = it.imageUrl, title = it.title) },
            messageBlocks = blocks,
            messageBlocksRestoreGeneration = restoreGeneration,
        )
    }

    companion object {
        fun from(form: EditorFormState): EditorFormSnapshot =
            EditorFormSnapshot(
                loadedItemId = form.loadedItemId,
                categoryName = form.selectedCategory.name,
                selectedService = form.selectedService,
                receivers =
                    form.afternoteEditReceivers.map {
                        ReceiverSnap(id = it.id, name = it.name, label = it.label)
                    },
                social = form.socialProcessingMethods.map { PmSnap(it.id, it.text) },
                gallery = form.galleryProcessingMethods.map { PmSnap(it.id, it.text) },
                selectedLastWish = form.selectedLastWish,
                pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                funeralVideoUrl = form.funeralVideoUrl,
                funeralThumbnailUrl = form.funeralThumbnailUrl,
                memorialPhotoUrl = form.memorialPhotoUrl,
                playlistSongCount = form.playlistSongCount,
                memorialPlaylistSongs = form.memorialPlaylistSongs,
                albumCovers =
                    form.playlistAlbumCovers.map {
                        AlbumSnap(id = it.id, imageUrl = it.imageUrl, title = it.title)
                    },
                editorMessages =
                    form.messageBlocks.map { MessageBlockSnap(title = it.title, body = it.body) },
            )
    }
}

/**
 * 애프터노트 생성/수정 ViewModel. 저장은 [AfternoteRepository] 직접 호출, 미디어 해석은 [ResolveMemorialMediaForSaveUseCase] 가 담당.
 *
 * **단일 UI 상태:** 폼·작성자 수신자·저장 진행/오류는 단일 [AfternoteEditorUiState] 로 묶어 [uiState] 로만 노출한다
 * (CLAUDE.md UI Layer 규칙: *"한 화면당 단일 UI State 객체. loading/error/data 독립 스트림 분리 금지"*).
 * UI 일회성(저장 성공·썸네일 업로드·프리필 적용)은 [events] [Channel] 로 분리.
 *
 * **SSOT:** 비즈니스 폼 필드는 [EditorFormState] 로 [internalState] 안에 보관하며, 프로세스 종료 시
 * [SavedStateHandle] JSON 스냅샷으로 복원한다. 추모 플레이리스트 곡 목록은 폼의 [EditorFormState.memorialPlaylistSongs] 와
 * 그래프 스코프 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] StateFlow 가
 * 동기화된 뒤 스냅샷에 포함된다.
 *
 * **UI Layer 분리:** ViewModel은 Compose UI 객체(`TextFieldState`, `SnapshotStateList`, 파사드)를 들지 않는다.
 * UI 레이어는 `rememberAfternoteEditorState(getCurrentForm = ::currentForm, updateForm = ::updateForm)` 으로
 * 자체 파사드를 만들고, prefill 등 UI 상태 변경은 [AfternoteEditorEvent.PrefillLoaded] 이벤트로 위임받는다.
 *
 * 수정 모드(`itemId` 있음)의 상세 로드는 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel]
 * 과 같이 `init`에서만 트리거한다 (`LaunchedEffect`로 네비게이션에 위임하지 않음).
 * 서버 원본 카테고리(저장 API용)는 전용 [SavedStateHandle] 키에 보관해 폼 JSON과 함께 프로세스 데스 후 복원된다.
 * 저장 API의 HTTP·에러 바디 해석은 [com.afternote.feature.afternote.domain.repository.author.AfternoteRepository] 구현에서
 * 도메인 예외로 변환되며, 여기서는 Retrofit 타입을 알지 않는다.
 * UI 액션은 개별 public 메서드로 노출한다 (작성자 홈 화면 ViewModel과 동일).
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
    ) : ViewModel() {
        private val formSnapshotJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        private val internalState =
            MutableStateFlow(
                InternalState(
                    form = readFormSnapshotOrDefault(),
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

        /**
         * UI 레이어(파사드)에서 폼 상태를 단방향으로 갱신할 때 호출하는 인텐트.
         * SavedState 스냅샷 직렬화도 함께 수행한다.
         */
        fun updateForm(block: (EditorFormState) -> EditorFormState) {
            internalState.update { prev -> prev.copy(form = block(prev.form)) }
            persistFormSnapshot(internalState.value.form)
        }

        init {
            viewModelScope.launch {
                authorReceiverRepository
                    .observeReceivers()
                    .map { it.toAfternoteEditorReceivers() }
                    .collect { mapped ->
                        internalState.update { it.copy(authorReceivers = mapped) }
                    }
            }
            val editItemId = readEditItemId()
            if (editItemId == null) {
                savedStateHandle.remove<String>(EDITOR_ORIGINAL_CATEGORY_FOR_API_KEY)
            } else {
                loadExistingAfternoteForEdit(editItemId)
            }
        }

        private fun readEditItemId(): Long? = savedStateHandle.get<String>(NAV_ARG_ITEM_ID)?.toLongOrNull()

        private fun readFormSnapshotOrDefault(): EditorFormState {
            val raw = savedStateHandle.get<String>(EDITOR_FORM_SNAPSHOT_KEY) ?: return EditorFormState()
            return runCatching {
                formSnapshotJson
                    .decodeFromString(EditorFormSnapshot.serializer(), raw)
                    .toEditorFormState(restoreGeneration = System.nanoTime())
            }.getOrElse {
                Log.w(TAG, "readFormSnapshotOrDefault: decode failed, using defaults", it)
                EditorFormState()
            }
        }

        /** [EditorFormSnapshot] 직렬화. 실패 시 로그만 남긴다(용량 초과 등은 [EditorFormSnapshot] KDoc 참고). */
        private fun persistFormSnapshot(form: EditorFormState) {
            runCatching {
                savedStateHandle[EDITOR_FORM_SNAPSHOT_KEY] =
                    formSnapshotJson.encodeToString(EditorFormSnapshot.serializer(), EditorFormSnapshot.from(form))
            }.onFailure { e ->
                Log.w(TAG, "persistFormSnapshot failed", e)
            }
        }

        fun refreshAuthorReceivers() {
            viewModelScope.launch {
                authorReceiverRepository.refreshReceivers()
            }
        }

        fun uploadMemorialThumbnail(jpegBytes: ByteArray) {
            viewModelScope.launch {
                memorialThumbnailUploadRepository
                    .uploadThumbnail(jpegBytes)
                    .onSuccess { url ->
                        Log.d(TAG, "uploadMemorialThumbnail: success, url=$url")
                        internalState.update { it.copy(pendingThumbnailUrl = url) }
                    }.onFailure { e ->
                        Log.e(TAG, "uploadMemorialThumbnail: failed", e)
                    }
            }
        }

        fun saveAfternote(
            editingId: Long?,
            category: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistSongs: List<Song>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            if (internalState.value.isSaving) {
                Log.w(TAG, "saveAfternote: already saving, ignoring duplicate call")
                return
            }

            val validationError =
                AfternoteEditorValidator.validate(
                    category = category,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
                )
            if (validationError != null) {
                Log.w(TAG, "saveAfternote: validation failed: $validationError")
                internalState.update { it.copy(validationError = validationError) }
                return
            }

            val originalCategoryForApi = readOriginalCategoryForApiFromSavedState()
            val categoryForApi =
                if (editingId != null) (originalCategoryForApi ?: category) else category

            Log.d(
                TAG,
                "saveAfternote: editingId=$editingId, category=${categoryForApi.serverValue}, " +
                    "serviceName=${payload.serviceName}",
            )

            viewModelScope.launch {
                internalState.update {
                    it.copy(isSaving = true, error = null, errorRes = null, validationError = null)
                }
                buildSaveCommand(
                    editingId = editingId,
                    categoryForApi = categoryForApi,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
                    memorialMedia = memorialMedia,
                ).fold(
                    onSuccess = { command ->
                        executeSaveCommand(command).fold(
                            onSuccess = { id ->
                                Log.d(TAG, "saveAfternote: SUCCESS, savedId=$id")
                                internalState.update {
                                    it.copy(
                                        isSaving = false,
                                        savedId = id,
                                        pendingSaveSuccessId = id,
                                    )
                                }
                            },
                            onFailure = { e -> handleSaveFailure(e, categoryForApi) },
                        )
                    },
                    onFailure = { e -> handleSaveFailure(e, categoryForApi) },
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
                        is CreateAfternoteInput.Gallery -> afternoteRepository.createGallery(input.payload)
                        is CreateAfternoteInput.Playlist -> afternoteRepository.createPlaylist(input.payload)
                    }
                }

                is SaveAfternoteCommand.Update -> {
                    afternoteRepository.update(command.id, command.payload)
                }
            }

        private suspend fun buildSaveCommand(
            editingId: Long?,
            categoryForApi: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistSongs: List<Song>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ): Result<SaveAfternoteCommand> {
            val resolved =
                resolveMemorialMediaForSave(
                    funeralVideoUrl = memorialMedia.funeralVideoUrl,
                    memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                    pickedMemorialPhotoUri = memorialMedia.pickedMemorialPhotoUri,
                    funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
                    isUpdate = editingId != null,
                ).getOrElse { return Result.failure(it) }

            val command =
                if (editingId != null) {
                    val updatePayload =
                        AfternoteEditorFormMapper.buildUpdatePayload(
                            category = categoryForApi,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            memorialMedia =
                                MemorialMediaUrls(
                                    funeralVideoUrl = resolved.videoUrlForUpdate,
                                    funeralThumbnailUrl = resolved.funeralThumbnailUrlForUpdate,
                                    memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                                ),
                        )
                    SaveAfternoteCommand.Update(id = editingId, payload = updatePayload)
                } else {
                    val createInput =
                        AfternoteEditorFormMapper.buildCreateInput(
                            category = categoryForApi,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            funeralVideoUrl = resolved.resolvedVideoUrl,
                            funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
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
                        savedStateHandle[EDITOR_ORIGINAL_CATEGORY_FOR_API_KEY] = prefill.category.name
                        // UI 레이어 파사드가 TextFieldState·SnapshotStateList 등 UI 상태를 갱신하도록 위임.
                        // skeleton 종료는 UI 가 prefill 적용을 마친 뒤 [markPrefillApplied] 로 통보한다
                        // (uiState 갱신과 이벤트 처리가 별 스트림이라 여기서 끄면 skeleton 사라짐 → 빈 폼 → prefill 깜빡임 발생).
                        internalState.update { it.copy(pendingPrefill = prefill) }
                    }.onFailure { e ->
                        Log.e(TAG, "loadExistingAfternoteForEdit: id=$afternoteId failed", e)
                        // 실패 시 skeleton 에 갇히지 않도록 즉시 종료.
                        internalState.update { it.copy(isPrefillLoading = false) }
                    }
            }
        }

        /** UI 가 prefill 을 폼·텍스트 상태에 모두 반영한 직후 호출 → skeleton 종료. */
        fun markPrefillApplied() {
            internalState.update { it.copy(isPrefillLoading = false) }
        }

        private fun handleSaveFailure(
            e: Throwable,
            category: EditorCategory,
        ) {
            Log.e(TAG, "saveAfternote: FAILURE, category=${category.serverValue}", e)
            val validationError =
                when (e) {
                    is AfternoteAuthoringValidationException -> {
                        when (e.kind) {
                            AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED -> {
                                AfternoteValidationError.RECEIVERS_REQUIRED
                            }
                        }
                    }

                    is AfternoteValidationException -> {
                        e.validationError
                    }

                    else -> {
                        null
                    }
                }
            val errorMessage = if (validationError == null) e.message else null
            val errorRes =
                if (validationError == null && errorMessage == null) {
                    R.string.afternote_editor_save_failed_generic
                } else {
                    null
                }
            internalState.update {
                it.copy(
                    isSaving = false,
                    validationError = validationError,
                    error = errorMessage,
                    errorRes = errorRes,
                )
            }
        }

        fun getReceiverById(id: Long): AuthorReceiverEntry? = authorReceiverRepository.currentReceivers().find { it.receiverId == id }

        private fun readOriginalCategoryForApiFromSavedState(): EditorCategory? =
            savedStateHandle
                .get<String>(EDITOR_ORIGINAL_CATEGORY_FOR_API_KEY)
                ?.let { name -> runCatching { EditorCategory.valueOf(name) }.getOrNull() }

        // region Internal state shaping

        /**
         * VM 내부에서만 다루는 평탄한 상태.
         * public [AfternoteEditorUiState] 는 이 값을 [toUiState] 로 매핑해 노출한다.
         */
        private data class InternalState(
            val form: EditorFormState = EditorFormState(),
            val authorReceivers: List<AfternoteEditorReceiver> = emptyList(),
            val isSaving: Boolean = false,
            val isPrefillLoading: Boolean = false,
            val savedId: Long? = null,
            val validationError: AfternoteValidationError? = null,
            val error: String? = null,
            val errorRes: Int? = null,
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
                validationError = validationError,
                error = error,
                errorRes = errorRes,
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

        fun onPrefillConsumed() {
            internalState.update { it.copy(pendingPrefill = null) }
        }

        // endregion
    }
