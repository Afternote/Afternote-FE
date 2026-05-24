package com.afternote.feature.timeletter.presentation.viewmodel

import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeLetterWriteViewModel
    @Inject
    constructor(
        private val createTimeLetterUseCase: CreateTimeLetterUseCase,
        private val timeLetterRepository: TimeLetterRepository,
        private val userRepository: UserRepository,
        private val fileMetadataRepository: FileMetadataRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TimeLetterWriteUiState())
        val uiState: StateFlow<TimeLetterWriteUiState> = _uiState.asStateFlow()

        private var receiverNameMap: Map<Long, String> = emptyMap()

        init {
            viewModelScope.launch {
                receiverNameMap = runCatching { userRepository.getReceivers() }
                    .getOrElse { emptyList() }
                    .associate { it.receiverId to it.name }
            }
            loadDraftCount()
            observeRecipientResult()
        }

        private fun observeRecipientResult() {
            savedStateHandle
                .getStateFlow<LongArray?>("recipient_ids", null)
                .filterNotNull()
                .onEach { ids -> setRecipients(ids.toList()) }
                .launchIn(viewModelScope)
        }

        fun setRecipients(ids: List<Long>) {
            _uiState.update { state ->
                state.copy(
                    recipientIds = ids,
                    recipientNames = ids.mapNotNull { receiverNameMap[it] },
                )
            }
        }

        fun setSendAt(sendAt: String) {
            _uiState.update { it.copy(sendAt = sendAt) }
        }

        fun setSendTime(
            hour: Int,
            minute: Int,
        ) {
            val amPm = if (hour < 12) "오전" else "오후"
            val displayHour =
                when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
            val display = "$amPm $displayHour:${minute.toString().padStart(2, '0')}"
            _uiState.update { it.copy(sendTime = display, sendHour = hour, sendMinute = minute) }
        }

        fun saveDraft(
            title: String,
            textContents: Map<Long, String>,
        ) {
            save(title = title, textContents = textContents, status = TimeLetterStatus.DRAFT)
        }

        fun register(
            title: String,
            textContents: Map<Long, String>,
        ) {
            if (_uiState.value.sendAt == null) {
                _uiState.update { it.copy(errorMessage = "발송 날짜를 선택해주세요.") }
                return
            }
            save(title = title, textContents = textContents, status = TimeLetterStatus.SCHEDULED)
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun onSavedAsDraftShown() {
            _uiState.update { it.copy(savedAsDraft = false) }
        }

        fun onRegisteredShown() {
            _uiState.update { it.copy(registered = false) }
        }

        fun setTextAlign(align: TextAlign) {
            _uiState.update { it.copy(textAlign = align) }
        }

        fun setFocusedBlock(id: Long?) {
            _uiState.update { it.copy(focusedBlockId = id) }
        }

        fun addImageBlock(uri: Uri) {
            viewModelScope.launch {
                val name = fileMetadataRepository.getFileName(uri.toString())
                addMediaBlockInternal { id -> EditorBlock.Image(id, uri, name) }
            }
        }

        fun addAudioBlock(uri: Uri) {
            viewModelScope.launch {
                val name = fileMetadataRepository.getFileName(uri.toString())
                addMediaBlockInternal { id -> EditorBlock.Audio(id, uri, name) }
            }
        }

        fun addFileBlock(uri: Uri) {
            viewModelScope.launch {
                val name = fileMetadataRepository.getFileName(uri.toString())
                addMediaBlockInternal { id -> EditorBlock.File(id, uri, name) }
            }
        }

        fun addLinkBlock(url: String) {
            addMediaBlockInternal { id -> EditorBlock.Link(id, url) }
        }

        private fun addMediaBlockInternal(createBlock: (Long) -> EditorBlock) {
            _uiState.update { state ->
                val blocks = state.editorBlocks.toMutableList()
                val insertAfterIndex =
                    state.focusedBlockId
                        ?.let { focusedId -> blocks.indexOfFirst { it.id == focusedId }.takeIf { it >= 0 } }
                        ?: blocks.lastIndex

                var nextId = state.nextBlockId
                val mediaBlock = createBlock(nextId++)
                blocks.add(insertAfterIndex + 1, mediaBlock)

                val newTextBlock = EditorBlock.Text(nextId++)
                blocks.add(insertAfterIndex + 2, newTextBlock)

                state.copy(
                    editorBlocks = blocks,
                    focusedBlockId = newTextBlock.id,
                    nextBlockId = nextId,
                )
            }
        }

        fun removeBlock(id: Long) {
            _uiState.update { state ->
                val filtered = state.editorBlocks.filter { it.id != id }
                if (filtered.isEmpty()) {
                    val newTextId = state.nextBlockId
                    state.copy(
                        editorBlocks = listOf(EditorBlock.Text(newTextId)),
                        nextBlockId = newTextId + 1,
                    )
                } else {
                    state.copy(editorBlocks = filtered)
                }
            }
        }

        private fun save(
            title: String,
            textContents: Map<Long, String>,
            status: TimeLetterStatus,
        ) {
            val state = _uiState.value
            if (state.isSaving) return

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                val blocks = mapToBlockInputs(state.editorBlocks, textContents)
                val sendAt =
                    state.sendAt?.let { date ->
                        "${date}T${state.sendHour.toString().padStart(2, '0')}:${state.sendMinute.toString().padStart(2, '0')}:00"
                    }
                createTimeLetterUseCase(
                    title = title.ifBlank { null },
                    blocks = blocks,
                    sendAt = sendAt,
                    status = status,
                    receiverIds = state.recipientIds.ifEmpty { null },
                ).onSuccess {
                    if (status == TimeLetterStatus.DRAFT) {
                        loadDraftCount()
                        _uiState.update { it.copy(savedAsDraft = true) }
                    } else {
                        _uiState.update { it.copy(registered = true) }
                    }
                }.onFailure {
                    _uiState.update { it.copy(errorMessage = "저장에 실패했어요. 다시 시도해주세요.") }
                }
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        private suspend fun mapToBlockInputs(
            editorBlocks: List<EditorBlock>,
            textContents: Map<Long, String>,
        ): List<BlockInput> = buildList {
            for (block in editorBlocks) {
                when (block) {
                    is EditorBlock.Text -> {
                        val content = textContents[block.id] ?: ""
                        if (content.isNotBlank()) add(BlockInput.Text(content))
                    }
                    is EditorBlock.Image -> add(
                        BlockInput.Media(
                            uriString = block.uri.toString(),
                            mimeType = fileMetadataRepository.getMimeType(block.uri.toString()),
                            blockType = TimeLetterBlockType.IMAGE,
                        ),
                    )
                    is EditorBlock.Audio -> add(
                        BlockInput.Media(
                            uriString = block.uri.toString(),
                            mimeType = fileMetadataRepository.getMimeType(block.uri.toString()),
                            blockType = TimeLetterBlockType.AUDIO,
                        ),
                    )
                    is EditorBlock.File -> add(
                        BlockInput.Media(
                            uriString = block.uri.toString(),
                            mimeType = fileMetadataRepository.getMimeType(block.uri.toString()),
                            blockType = TimeLetterBlockType.FILE,
                        ),
                    )
                    is EditorBlock.Link -> add(BlockInput.Link(block.url))
                }
            }
        }

        private fun loadDraftCount() {
            viewModelScope.launch {
                runCatching { timeLetterRepository.getTemporaryTimeLetters() }
                    .onSuccess { result ->
                        _uiState.update { it.copy(draftCount = result.totalCount) }
                    }
            }
        }
    }
