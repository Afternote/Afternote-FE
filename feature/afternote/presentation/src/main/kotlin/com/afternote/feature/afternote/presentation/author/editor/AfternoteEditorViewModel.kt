package com.afternote.feature.afternote.presentation.author.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.AuthorReceiverRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
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

private const val EDITOR_FORM_SNAPSHOT_KEY = "editor_form_snapshot_v1"

/** 수정 진입 시 서버 원본 카테고리(API `categoryForApi`). 폼 스냅샷과 별도로 두어 프로세스 데스 후에도 유지한다. */
private const val EDITOR_ORIGINAL_CATEGORY_FOR_API_KEY = "editor_original_category_for_api_v1"

/** 타입 안전 [com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute.EditorRoute] 직렬화 인자명 (상세 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel]과 동일). */
private const val NAV_ARG_ITEM_ID = "itemId"

private const val TAG = "AfternoteEditorViewModel"

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
    val methods: List<PmSnap> = emptyList(),
    val selectedLastWish: String? = null,
    val pickedMemorialPhotoUri: String? = null,
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val playlistSongCount: Int = 0,
    val memorialPlaylistSongs: List<Song> = emptyList(),
    val albumCovers: List<AlbumSnap> = emptyList(),
    val editorMessages: List<MessageBlockSnap> = emptyList(),
) {
    fun toEditorFormState(restoreGeneration: Long): EditorFormState {
        val category =
            runCatching { EditorCategory.valueOf(categoryName) }.getOrElse { EditorCategory.SOCIAL }
        val blocks: List<EditorMessageTextBlock> =
            if (editorMessages.isEmpty()) {
                DEFAULT_EDITOR_MESSAGE_BLOCKS
            } else {
                editorMessages.map { EditorMessageTextBlock(title = it.title, body = it.body) }
            }
        return EditorFormState(
            loadedItemId = loadedItemId,
            selectedCategory = category,
            // 스냅샷의 빈 문자열은 미선택(null)로 복원 — process death 후에도 임의 기본값을 확정하지 않는다 (이슈 #468).
            selectedService = selectedService.ifBlank { null },
            afternoteEditReceivers =
                receivers.map { AfternoteEditorReceiver(id = it.id, name = it.name, label = it.label) },
            processingMethods = methods.map { ProcessingMethodItem(it.id, it.text) },
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
                selectedService = form.selectedService.orEmpty(),
                receivers =
                    form.afternoteEditReceivers.map {
                        ReceiverSnap(id = it.id, name = it.name, label = it.label)
                    },
                methods = form.processingMethods.map { PmSnap(it.id, it.text) },
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
 * **단일 UI 상태:** 폼·작성자 수신자·저장 진행/오류·일회성 신호 모두 단일 [AfternoteEditorUiState] 로 묶어 [uiState] 로만 노출한다
 * (Google 공식 가이드 — *"ViewModel events should always result in a UI state update"*).
 * 일회성(저장 성공·썸네일 업로드·프리필 적용)은 UiState 의 nullable 신호로 표현하고 UI 가 `LaunchedEffect` 로 소비 후
 * `on*Consumed` 콜백으로 reset.
 *
 * **SSOT:** 비즈니스 폼 필드는 [EditorFormState] 로 [internalState] 안에 보관하며, 프로세스 종료 시
 * [SavedStateHandle] JSON 스냅샷으로 복원한다. 추억 플레이리스트 곡 목록은 폼의 [EditorFormState.memorialPlaylistSongs] 와
 * 그래프 스코프 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] StateFlow 가
 * 동기화된 뒤 스냅샷에 포함된다.
 *
 * **UI Layer 분리:** ViewModel은 Compose UI 객체(`TextFieldState`, `SnapshotStateList`, 파사드)를 들지 않는다.
 * UI 레이어는 `rememberAfternoteEditorState(getCurrentForm = ::currentForm, updateForm = ::updateForm)` 으로
 * 자체 파사드를 만들고, prefill 등 UI 상태 변경은 [AfternoteEditorUiState.pendingPrefill] 신호로 위임받아 적용 후
 * [onPrefillConsumed] 로 통보한다.
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
        private val errorReporter: ErrorReporter,
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
            }.getOrElse { EditorFormState() }
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

        fun uploadMemorialThumbnail(jpegBytes: ByteArray) {
            viewModelScope.launch {
                memorialThumbnailUploadRepository
                    .uploadThumbnail(jpegBytes)
                    .onSuccess { url ->
                        Log.d(TAG, "uploadMemorialThumbnail: success, url=$url")
                        internalState.update { it.copy(pendingThumbnailUrl = url) }
                    }.onFailure { e ->
                        Log.e(TAG, "uploadMemorialThumbnail: failed", e)
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_UPLOAD, e)
                        internalState.update { it.copy(thumbnailUploadFailed = true) }
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
            editingId: Long?,
            category: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistSongs: List<Song>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            if (internalState.value.isSaving) return

            val validationError =
                AfternoteEditorValidator.validate(
                    category = category,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
                )
            if (validationError != null) {
                internalState.update { it.copy(validationError = validationError) }
                return
            }

            val originalCategoryForApi = readOriginalCategoryForApiFromSavedState()
            val categoryForApi =
                if (editingId != null) (originalCategoryForApi ?: category) else category

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
                        is CreateAfternoteInput.Playlist -> afternoteRepository.createPlaylist(input.payload)
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
            categoryForApi: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistSongs: List<Song>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ): Result<SaveAfternoteCommand> {
            val resolved =
                resolveMemorialMediaForSave(
                    video = videoMediaInput(memorialMedia.funeralVideoUrl),
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
                            category = categoryForApi,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            memorialMedia =
                                MemorialMediaUrls(
                                    funeralVideoUrl = resolved.resolvedVideoUrl,
                                    funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
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
                        // skeleton 종료는 UI 가 prefill 적용을 마친 뒤 [onPrefillConsumed] 로 통보한다
                        // (uiState 갱신 시점에 prefill 도착했어도 UI 가 form·TextFieldState 에 반영하기 전이라
                        //  여기서 끄면 skeleton 사라짐 → 빈 폼 → prefill 깜빡임 발생).
                        internalState.update { it.copy(pendingPrefill = prefill) }
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
            // 입력 검증 실패(수신자 미선택 등)는 사용자가 고칠 정상 경로라 기록하지 않는다 —
            // 보관 한도(최근 8건) 를 사용자 오류가 차지하면 실제 등록 장애가 밀려난다.
            if (validationError == null) {
                errorReporter.recordAfternoteFailure(AfternoteFailureStage.SAVE, e)
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
            val thumbnailUploadFailed: Boolean = false,
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
                thumbnailUploadFailed = thumbnailUploadFailed,
                pendingPrefill = pendingPrefill,
            )

        fun onSaveSuccessConsumed() {
            internalState.update { it.copy(pendingSaveSuccessId = null) }
        }

        fun onThumbnailUploadedConsumed() {
            internalState.update { it.copy(pendingThumbnailUrl = null) }
        }

        fun onThumbnailUploadErrorConsumed() {
            internalState.update { it.copy(thumbnailUploadFailed = false) }
        }

        // endregion
    }
