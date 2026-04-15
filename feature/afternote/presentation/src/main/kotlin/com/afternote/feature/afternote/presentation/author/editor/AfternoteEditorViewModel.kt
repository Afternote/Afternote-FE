package com.afternote.feature.afternote.presentation.author.editor

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.AuthorReceiverRepository
import com.afternote.feature.afternote.domain.repository.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.presentation.author.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationException
import com.afternote.feature.afternote.presentation.author.editor.state.DEFAULT_EDITOR_MESSAGE_BLOCKS
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "AfternoteEditorVM"

private const val EDITOR_FORM_SNAPSHOT_KEY = "editor_form_snapshot_v1"

/** 타입 안전 [com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute.EditorRoute] 직렬화 인자명 (상세 [AfternoteDetailViewModel]과 동일). */
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
    val processingAccount: String = "MEMORIAL_ACCOUNT",
    val processingInfo: String = "TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER",
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
        val accountMethod =
            runCatching { AccountProcessingMethod.valueOf(processingAccount) }
                .getOrElse { AccountProcessingMethod.MEMORIAL_ACCOUNT }
        val infoMethod =
            runCatching { InformationProcessingMethod.valueOf(processingInfo) }
                .getOrElse { InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER }
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
            selectedProcessingMethod = accountMethod,
            selectedInformationProcessingMethod = infoMethod,
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
                processingAccount = form.selectedProcessingMethod.name,
                processingInfo = form.selectedInformationProcessingMethod.name,
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
 * 애프터노트 생성/수정 ViewModel. 저장·미디어 해석은 [SaveAfternoteOrchestrator]에 위임합니다.
 *
 * **SSOT:** 비즈니스 폼 필드는 [EditorFormState] + [editorFormStateFlow]이며, 프로세스 종료 시 [SavedStateHandle] JSON 스냅샷으로 복원합니다.
 * 추모 플레이리스트 곡 목록은 폼의 [EditorFormState.memorialPlaylistSongs]와 그래프 스코프 [MemorialPlaylistStateHolder]가 동기화된 뒤 스냅샷에 포함됩니다.
 * 순수 UI는 [editorUi] ([AfternoteEditorUiState])가 담당합니다.
 *
 * 수정 모드(`itemId` 있음)의 상세 로드는 [AfternoteDetailViewModel]과 같이 `init`에서만 트리거한다 (`LaunchedEffect`로 네비게이션에 위임하지 않음).
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
        private val saveAfternoteOrchestrator: SaveAfternoteOrchestrator,
    ) : ViewModel() {
        private val formSnapshotJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        val editorUi: AfternoteEditorUiState =
            AfternoteEditorUiState(
                idState = TextFieldState(),
                passwordState = TextFieldState(),
                afternoteEditReceiverNameState = TextFieldState(),
                phoneNumberState = TextFieldState(),
                customServiceNameState = TextFieldState(),
                customLastWishState = TextFieldState(),
            )

        private val editorForm =
            MutableStateFlow(readFormSnapshotOrDefault())

        /** 비즈니스 폼 상태 (UDF 소스). */
        val editorFormStateFlow: StateFlow<EditorFormState> = editorForm.asStateFlow()

        val editorFormState: AfternoteEditorState =
            AfternoteEditorState(
                ui = editorUi,
                updateForm = { block ->
                    editorForm.update(block)
                    persistFormSnapshot(editorForm.value)
                },
                formStateSource = editorForm.asStateFlow(),
            )

        private val apiErrorBodyJson =
            Json {
                ignoreUnknownKeys = true
            }

        private val _saveState = MutableStateFlow(AfternoteSaveState())
        val saveState: StateFlow<AfternoteSaveState> = _saveState.asStateFlow()

        private val _events = Channel<AfternoteEditorEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private val _authorReceiversUi = MutableStateFlow<List<AfternoteEditorReceiver>>(emptyList())
        val authorReceiversUi: StateFlow<List<AfternoteEditorReceiver>> =
            _authorReceiversUi.asStateFlow()

        init {
            viewModelScope.launch {
                authorReceiverRepository
                    .observeReceivers()
                    .map { it.toAfternoteEditorReceivers() }
                    .collect { mapped -> _authorReceiversUi.value = mapped }
            }
            val editItemId = savedStateHandle.get<String>(NAV_ARG_ITEM_ID)?.toLongOrNull()
            if (editItemId != null) {
                viewModelScope.launch {
                    loadExistingAfternoteForEdit(editItemId)
                }
            }
        }

        private var loadedCategoryForEdit: EditorCategory? = null

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
                        _events.send(AfternoteEditorEvent.ThumbnailUploaded(url))
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
            playlistStateHolder: MemorialPlaylistStateHolder?,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            if (_saveState.value.isSaving) {
                Log.w(TAG, "saveAfternote: already saving, ignoring duplicate call")
                return
            }

            val validationError =
                AfternoteEditorValidator.validate(
                    category = category,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistStateHolder = playlistStateHolder,
                )
            if (validationError != null) {
                Log.w(TAG, "saveAfternote: validation failed: $validationError")
                _saveState.update { it.copy(validationError = validationError) }
                return
            }

            val categoryForApi =
                if (editingId != null) (loadedCategoryForEdit ?: category) else category

            Log.d(
                TAG,
                "saveAfternote: editingId=$editingId, category=${categoryForApi.serverValue}, " +
                    "serviceName=${payload.serviceName}",
            )

            viewModelScope.launch {
                _saveState.update {
                    it.copy(isSaving = true, error = null, validationError = null)
                }
                saveAfternoteOrchestrator(
                    editingId = editingId,
                    categoryForApi = categoryForApi,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistStateHolder = playlistStateHolder,
                    memorialMedia = memorialMedia,
                ).fold(
                    onSuccess = { id ->
                        Log.d(TAG, "saveAfternote: SUCCESS, savedId=$id")
                        _saveState.update {
                            it.copy(isSaving = false, savedId = id)
                        }
                        _events.send(AfternoteEditorEvent.SaveSuccess(id))
                    },
                    onFailure = { e -> handleSaveFailure(e, categoryForApi) },
                )
            }
        }

        private fun loadExistingAfternoteForEdit(afternoteId: Long) {
            viewModelScope.launch {
                afternoteRepository
                    .getDetail(id = afternoteId)
                    .onSuccess { detail ->
                        val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(detail)
                        loadedCategoryForEdit = prefill.category
                        editorFormState.applyFormPrefill(prefill)
                    }
            }
        }

        private fun handleSaveFailure(
            e: Throwable,
            category: EditorCategory,
        ) {
            Log.e(TAG, "saveAfternote: FAILURE, category=${category.serverValue}", e)
            val validationError =
                when (e) {
                    is AfternoteValidationException -> e.validationError
                    is HttpException if e.code() == 400 -> parseReceiversRequiredFromBody(e)
                    else -> null
                }
            val errorMessage =
                when {
                    validationError != null -> null
                    else -> e.message ?: "저장에 실패했습니다."
                }
            _saveState.update {
                it.copy(
                    isSaving = false,
                    validationError = validationError,
                    error = errorMessage,
                )
            }
        }

        fun getReceiverById(id: Long): AuthorReceiverEntry? = authorReceiverRepository.currentReceivers().find { it.receiverId == id }

        private fun parseReceiversRequiredFromBody(e: HttpException): AfternoteValidationError? {
            val body = e.response()?.errorBody()?.string() ?: return null
            return runCatching {
                val parsed = apiErrorBodyJson.decodeFromString<ApiErrorBody>(body)
                if (parsed.code == 475) AfternoteValidationError.RECEIVERS_REQUIRED else null
            }.getOrNull()
        }
    }

@Serializable
private data class ApiErrorBody(
    val code: Int? = null,
)
