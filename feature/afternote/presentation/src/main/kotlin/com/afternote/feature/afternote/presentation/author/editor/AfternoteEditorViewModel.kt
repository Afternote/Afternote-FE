package com.afternote.feature.afternote.presentation.author.editor

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.author.AuthorReceiverEntry
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.AuthorReceiverRepository
import com.afternote.feature.afternote.domain.repository.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.presentation.author.editor.mapper.AfternoteEditorMapper
import com.afternote.feature.afternote.presentation.author.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteSaveState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationException
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.usecase.SaveAfternoteUseCase
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

/**
 * 애프터노트 생성/수정 ViewModel. 저장·미디어 해석은 [SaveAfternoteUseCase]에 위임합니다.
 *
 * **SSOT:** 수정 모드에서 폼에 채울 원본은 홈 리스트가 아니라 [AfternoteRepository.getDetail] 응답이다.
 * Navigation은 [com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute.EditorRoute]의
 * `itemId` 등 **최소 식별자**만 넘기고, 실제 로드는 [loadForEdit]에서 Repository를 호출해 수행한다
 * (Modern Android Architecture 권장과 동일한 방향).
 */
@HiltViewModel
class AfternoteEditorViewModel
    @Inject
    constructor(
        private val authorReceiverRepository: AuthorReceiverRepository,
        private val afternoteRepository: AfternoteRepository,
        private val memorialThumbnailUploadRepository: MemorialThumbnailUploadRepository,
        private val saveAfternoteUseCase: SaveAfternoteUseCase,
    ) : ViewModel() {
        val editorFormState: AfternoteEditorState =
            AfternoteEditorState(
                idState = TextFieldState(),
                passwordState = TextFieldState(),
                afternoteEditReceiverNameState = TextFieldState(),
                phoneNumberState = TextFieldState(),
                customServiceNameState = TextFieldState(),
                customLastWishState = TextFieldState(),
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
        }

        private var loadedCategoryForEdit: EditorCategory? = null

        fun onEvent(event: AfternoteEditorUiEvent) {
            when (event) {
                is AfternoteEditorUiEvent.LoadReceivers -> loadReceivers()
                is AfternoteEditorUiEvent.UploadThumbnail -> uploadMemorialThumbnail(event.jpegBytes)
                is AfternoteEditorUiEvent.Save ->
                    saveAfternote(
                        editingId = event.editingId,
                        category = event.editorCategory,
                        payload = event.payload,
                        selectedReceiverIds = event.selectedReceiverIds,
                        playlistStateHolder = event.playlistStateHolder,
                        memorialMedia = event.memorialMedia,
                    )

                is AfternoteEditorUiEvent.LoadForEdit ->
                    loadForEdit(
                        afternoteId = event.afternoteId,
                        state = event.state,
                        playlistStateHolder = event.playlistStateHolder,
                    )
            }
        }

        private fun loadReceivers() {
            viewModelScope.launch {
                authorReceiverRepository.refreshReceivers()
            }
        }

        private fun uploadMemorialThumbnail(jpegBytes: ByteArray) {
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

        private fun saveAfternote(
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
                saveAfternoteUseCase(
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

        /**
         * 서버(또는 API) 기준 최신 상세를 가져와 폼에 반영한다. 목록 스냅샷은 사용하지 않는다.
         *
         * [MemorialPlaylistStateHolder]는 그래프 스코프 인메모리 초안이라 DI로 VM에 주입되지 않으며,
         * 호출부(Compose)에서 [AfternoteEditorUiEvent.LoadForEdit]로 함께 넘긴다.
         */
        private fun loadForEdit(
            afternoteId: Long,
            state: AfternoteEditorState,
            playlistStateHolder: MemorialPlaylistStateHolder? = null,
        ) {
            viewModelScope.launch {
                afternoteRepository
                    .getDetail(id = afternoteId)
                    .onSuccess { detail ->
                        populatePlaylistFromDetail(detail, playlistStateHolder)
                        val params = AfternoteEditorMapper.buildLoadFromExistingParams(detail)
                        loadedCategoryForEdit =
                            EditorCategory.fromDisplayLabel(params.categoryDisplayString)
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

        private fun handleSaveFailure(
            e: Throwable,
            category: EditorCategory,
        ) {
            Log.e(TAG, "saveAfternote: FAILURE, category=${category.serverValue}", e)
            val validationError =
                when {
                    e is AfternoteValidationException -> e.validationError
                    e is HttpException && e.code() == 400 -> parseReceiversRequiredFromBody(e)
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
