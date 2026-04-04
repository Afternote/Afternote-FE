package com.afternote.feature.afternote.presentation.author.editor

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.author.AuthorReceiverDirectoryEntry
import com.afternote.feature.afternote.domain.port.AuthorReceiversDirectoryPort
import com.afternote.feature.afternote.domain.port.CurrentAuthorUserIdPort
import com.afternote.feature.afternote.domain.usecase.author.GetDetailUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.CreateGalleryUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.CreatePlaylistUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.CreateSocialUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.UpdateUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.UploadMemorialPhotoUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.UploadMemorialThumbnailUseCase
import com.afternote.feature.afternote.domain.usecase.author.editor.UploadMemorialVideoUseCase
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteValidationException
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.playlist.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

private const val TAG = "AfternoteEditorVM"

/** S3 presigned URLs contain this; we must not send them back on PATCH or the server overwrites the stored key. */
private const val PRESIGNED_URL_MARKER = "X-Amz-"

/** Content URI scheme; used to detect local picks that must be uploaded before save. */
private const val CONTENT_SCHEME = "content://"

/**
 * 애프터노트 생성/수정 ViewModel.
 *
 * 단발성 이벤트(저장 성공, 썸네일 업로드 완료)는 [Channel]을 통해 전달하여
 * 화면 회전 등 재구성 시 재실행되지 않도록 합니다.
 */
@HiltViewModel
class AfternoteEditorViewModel
    @Inject
    constructor(
        private val createSocialUseCase: CreateSocialUseCase,
        private val createGalleryUseCase: CreateGalleryUseCase,
        private val createPlaylistUseCase: CreatePlaylistUseCase,
        private val updateUseCase: UpdateUseCase,
        private val getDetailUseCase: GetDetailUseCase,
        private val currentAuthorUserId: CurrentAuthorUserIdPort,
        private val authorReceiversDirectory: AuthorReceiversDirectoryPort,
        private val uploadMemorialThumbnailUseCase: UploadMemorialThumbnailUseCase,
        private val uploadMemorialVideoUseCase: UploadMemorialVideoUseCase,
        private val uploadMemorialPhotoUseCase: UploadMemorialPhotoUseCase,
    ) : ViewModel() {
        private val _saveState = MutableStateFlow(AfternoteSaveState())
        val saveState: StateFlow<AfternoteSaveState> = _saveState.asStateFlow()

        /** 단발성 이벤트 채널: 저장 성공, 썸네일 업로드 완료 등. */
        private val _events = Channel<AfternoteEditorEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        /** Cached receiver list (GET /users/receivers) for lookup when returning from receiver selection. */
        private var cachedReceivers: List<AuthorReceiverDirectoryEntry> = emptyList()

        /**
         * Category from the server when loading for edit. Used for update requests because the API
         * does not allow changing category; we must send the original category.
         */
        private var loadedCategoryForEdit: EditorCategory? = null

        // region Event

        fun onEvent(event: AfternoteEditorUiEvent) {
            when (event) {
                is AfternoteEditorUiEvent.LoadReceivers -> {
                    loadReceivers()
                }

                is AfternoteEditorUiEvent.UploadThumbnail -> {
                    uploadMemorialThumbnail(event.jpegBytes)
                }

                is AfternoteEditorUiEvent.Save -> {
                    saveAfternote(
                        editingId = event.editingId,
                        category = event.category,
                        payload = event.payload,
                        selectedReceiverIds = event.selectedReceiverIds,
                        playlistStateHolder = event.playlistStateHolder,
                        memorialMedia = event.memorialMedia,
                    )
                }

                is AfternoteEditorUiEvent.LoadForEdit -> {
                    loadForEdit(
                        afternoteId = event.afternoteId,
                        state = event.state,
                        playlistStateHolder = event.playlistStateHolder,
                    )
                }
            }
        }

        // endregion

        // region Data Loading

        private fun loadReceivers() {
            viewModelScope.launch {
                val userId = currentAuthorUserId() ?: return@launch
                authorReceiversDirectory(userId)
                    .getOrNull()
                    ?.let { cachedReceivers = it }
            }
        }

        private fun uploadMemorialThumbnail(jpegBytes: ByteArray) {
            viewModelScope.launch {
                uploadMemorialThumbnailUseCase(jpegBytes)
                    .onSuccess { url ->
                        Log.d(TAG, "uploadMemorialThumbnail: success, url=$url")
                        _events.send(AfternoteEditorEvent.ThumbnailUploaded(url))
                    }.onFailure { e ->
                        Log.e(TAG, "uploadMemorialThumbnail: failed", e)
                    }
            }
        }

        /**
         * 애프터노트 저장 (생성 또는 수정).
         */
        private fun saveAfternote(
            editingId: Long?,
            category: String,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistStateHolder: MemorialPlaylistStateHolder?,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            if (_saveState.value.isSaving) {
                Log.w(TAG, "saveAfternote: already saving, ignoring duplicate call")
                return
            }

            val editorCategory = EditorCategory.fromDisplayLabel(category)
            val validationError =
                AfternoteEditorValidator.validate(
                    category = editorCategory,
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
                if (editingId != null) (loadedCategoryForEdit ?: editorCategory) else editorCategory

            Log.d(
                TAG,
                "saveAfternote: editingId=$editingId, category=${categoryForApi.serverValue}, " +
                    "serviceName=${payload.serviceName}",
            )

            viewModelScope.launch {
                _saveState.update {
                    it.copy(isSaving = true, error = null, validationError = null)
                }
                val resolvedVideoUrl = resolveVideoUrlForSave(memorialMedia.funeralVideoUrl)
                if (resolvedVideoUrl == null &&
                    memorialMedia.funeralVideoUrl != null &&
                    memorialMedia.funeralVideoUrl.startsWith(CONTENT_SCHEME)
                ) {
                    return@launch
                }
                val resolvedMemorialPhotoUrl =
                    resolveMemorialPhotoUrlForSave(
                        memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                        pickedMemorialPhotoUri = memorialMedia.pickedMemorialPhotoUri,
                    )
                if (resolvedMemorialPhotoUrl == null &&
                    memorialMedia.pickedMemorialPhotoUri != null &&
                    memorialMedia.pickedMemorialPhotoUri.startsWith(CONTENT_SCHEME)
                ) {
                    return@launch
                }
                val videoUrlForUpdate = videoUrlForUpdateRequest(editingId != null, resolvedVideoUrl)
                val thumbnailForUpdate =
                    if (videoUrlForUpdate == null) null else memorialMedia.funeralThumbnailUrl

                val result =
                    if (editingId != null) {
                        val updateInput =
                            AfternoteEditorMapper.buildUpdateInput(
                                category = categoryForApi,
                                payload = payload,
                                selectedReceiverIds = selectedReceiverIds,
                                playlistStateHolder = playlistStateHolder,
                                memorialMedia =
                                    MemorialMediaUrls(
                                        funeralVideoUrl = videoUrlForUpdate,
                                        funeralThumbnailUrl = thumbnailForUpdate,
                                        memorialPhotoUrl = resolvedMemorialPhotoUrl,
                                    ),
                            )
                        updateUseCase(id = editingId, updateInput = updateInput)
                    } else {
                        performCreate(
                            category = categoryForApi,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistStateHolder = playlistStateHolder,
                            funeralVideoUrl = resolvedVideoUrl,
                            funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
                            memorialPhotoUrl = resolvedMemorialPhotoUrl,
                        )
                    }

                result
                    .onSuccess { id ->
                        Log.d(TAG, "saveAfternote: SUCCESS, savedId=$id")
                        _saveState.update {
                            it.copy(isSaving = false, savedId = id)
                        }
                        _events.send(AfternoteEditorEvent.SaveSuccess(id))
                    }.onFailure { e ->
                        handleSaveFailure(e, categoryForApi)
                    }
            }
        }

        private fun loadForEdit(
            afternoteId: Long,
            state: AfternoteEditorState,
            playlistStateHolder: MemorialPlaylistStateHolder? = null,
        ) {
            viewModelScope.launch {
                getDetailUseCase(id = afternoteId)
                    .onSuccess { detail ->
                        populatePlaylistFromDetail(detail, playlistStateHolder)
                        val params = AfternoteEditorMapper.buildLoadFromExistingParams(detail)
                        loadedCategoryForEdit = EditorCategory.fromDisplayLabel(params.categoryDisplayString)
                        state.loadFromExisting(params)
                    }
            }
        }

        private fun populatePlaylistFromDetail(
            detail: Detail,
            playlistStateHolder: MemorialPlaylistStateHolder?,
        ) {
            val detailCategory = EditorCategory.fromServerValue(detail.category)
            if (detailCategory != EditorCategory.MEMORIAL || playlistStateHolder == null) return
            val playlist = detail.playlist ?: return
            playlistStateHolder.clearAllSongs()
            playlist.songs
                .mapIndexed { index, s ->
                    Song(
                        id = (s.id ?: index.toLong()).toString(),
                        title = s.title,
                        artist = s.artist,
                        albumCoverUrl = s.coverUrl,
                    )
                }.forEach { playlistStateHolder.addSong(it) }
        }

        private suspend fun performCreate(
            category: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistStateHolder: MemorialPlaylistStateHolder?,
            funeralVideoUrl: String?,
            funeralThumbnailUrl: String?,
            memorialPhotoUrl: String?,
        ): Result<Long> {
            val createInput =
                AfternoteEditorMapper.buildCreateInput(
                    category = category,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistStateHolder = playlistStateHolder,
                    funeralVideoUrl = funeralVideoUrl,
                    funeralThumbnailUrl = funeralThumbnailUrl,
                    memorialPhotoUrl = memorialPhotoUrl,
                )
            return when (createInput) {
                is CreateInput.Social -> createSocialUseCase(createInput.input)
                is CreateInput.Gallery -> createGalleryUseCase(createInput.input)
                is CreateInput.Playlist -> createPlaylistUseCase(createInput.input)
            }
        }

        private suspend fun resolveVideoUrlForSave(funeralVideoUrl: String?): String? {
            when {
                funeralVideoUrl.isNullOrBlank() -> return null
                !funeralVideoUrl.startsWith(CONTENT_SCHEME) -> return funeralVideoUrl
            }
            return uploadMemorialVideoUseCase(funeralVideoUrl).fold(
                onSuccess = { it },
                onFailure = { e ->
                    Log.e(TAG, "saveAfternote: video upload failed", e)
                    _saveState.update {
                        it.copy(isSaving = false, error = e.message ?: "영상 업로드에 실패했습니다.")
                    }
                    null
                },
            )
        }

        private suspend fun resolveMemorialPhotoUrlForSave(
            memorialPhotoUrl: String?,
            pickedMemorialPhotoUri: String?,
        ): String? {
            if (!pickedMemorialPhotoUri.isNullOrBlank() &&
                pickedMemorialPhotoUri.startsWith(CONTENT_SCHEME)
            ) {
                return uploadMemorialPhotoUseCase(pickedMemorialPhotoUri).fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        Log.e(TAG, "saveAfternote: memorial photo upload failed", e)
                        _saveState.update {
                            it.copy(isSaving = false, error = e.message ?: "영정 사진 업로드에 실패했습니다.")
                        }
                        null
                    },
                )
            }
            return memorialPhotoUrl?.takeIf { it.isNotBlank() }
        }

        private fun videoUrlForUpdateRequest(
            isUpdate: Boolean,
            resolvedVideoUrl: String?,
        ): String? {
            if (!isUpdate || resolvedVideoUrl == null) return resolvedVideoUrl
            if (resolvedVideoUrl.contains(PRESIGNED_URL_MARKER)) {
                Log.d(TAG, "saveAfternote: skipping videoUrl in PATCH (presigned URL)")
                return null
            }
            return resolvedVideoUrl
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

        // endregion

        // region Utility

        fun getReceiverById(id: Long): AuthorReceiverDirectoryEntry? = cachedReceivers.find { it.receiverId == id }

        private fun parseReceiversRequiredFromBody(e: HttpException): AfternoteValidationError? {
            val body = e.response()?.errorBody()?.string() ?: return null
            return runCatching {
                val parsed = Json.decodeFromString<ApiErrorBody>(body)
                if (parsed.code == 475) AfternoteValidationError.RECEIVERS_REQUIRED else null
            }.getOrNull()
        }

        // endregion
    }

/** 단발성 이벤트. [Channel]을 통해 한 번만 소비됩니다. */
sealed interface AfternoteEditorEvent {
    data class SaveSuccess(
        val savedId: Long,
    ) : AfternoteEditorEvent

    data class ThumbnailUploaded(
        val url: String,
    ) : AfternoteEditorEvent
}

/** UI에서 ViewModel로 전달되는 사용자 액션 이벤트. */
sealed interface AfternoteEditorUiEvent {
    data object LoadReceivers : AfternoteEditorUiEvent

    data class UploadThumbnail(
        val jpegBytes: ByteArray,
    ) : AfternoteEditorUiEvent

    data class Save(
        val editingId: Long?,
        val category: String,
        val payload: RegisterAfternotePayload,
        val selectedReceiverIds: List<Long>,
        val playlistStateHolder: MemorialPlaylistStateHolder?,
        val memorialMedia: SaveAfternoteMemorialMedia,
    ) : AfternoteEditorUiEvent

    data class LoadForEdit(
        val afternoteId: Long,
        val state: AfternoteEditorState,
        val playlistStateHolder: MemorialPlaylistStateHolder?,
    ) : AfternoteEditorUiEvent
}

/**
 * Memorial-related media URLs and the picked photo URI for save.
 */
data class SaveAfternoteMemorialMedia(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
    val pickedMemorialPhotoUri: String? = null,
)

/** API 400 응답 body 파싱용 (code 475 등). */
@Serializable
private data class ApiErrorBody(
    val code: Int? = null,
)
