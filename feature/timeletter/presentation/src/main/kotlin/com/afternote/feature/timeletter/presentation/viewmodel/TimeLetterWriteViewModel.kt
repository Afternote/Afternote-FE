package com.afternote.feature.timeletter.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.data.cache.ReceiverCacheStore
import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeLetterWriteViewModel
    @Inject
    constructor(
        private val timeLetterRepository: TimeLetterRepository,
        private val receiverCacheStore: ReceiverCacheStore,
        private val photoUploadRepository: PhotoUploadRepository,
        @param:ApplicationContext private val context: Context,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TimeLetterWriteUiState())
        val uiState: StateFlow<TimeLetterWriteUiState> = _uiState.asStateFlow()

        private val _events = Channel<TimeLetterWriteEvent>()
        val events = _events.receiveAsFlow()

        init {
            viewModelScope.launch { receiverCacheStore.ensureLoaded() }
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
            val nameMap = receiverCacheStore.receiverNameMap.value
            _uiState.update { state ->
                state.copy(
                    recipientIds = ids,
                    recipientNames = ids.mapNotNull { nameMap[it] },
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

        fun setTextAlign(align: TextAlign) {
            _uiState.update { it.copy(textAlign = align) }
        }

        fun setFocusedBlock(id: Long?) {
            _uiState.update { it.copy(focusedBlockId = id) }
        }

        fun addImageBlock(uri: Uri) {
            addMediaBlockInternal { id -> EditorBlock.Image(id, uri, getFileName(uri)) }
        }

        fun addAudioBlock(uri: Uri) {
            addMediaBlockInternal { id -> EditorBlock.Audio(id, uri, getFileName(uri)) }
        }

        fun addFileBlock(uri: Uri) {
            addMediaBlockInternal { id -> EditorBlock.File(id, uri, getFileName(uri)) }
        }

        fun addLinkBlock(url: String) {
            addMediaBlockInternal { id -> EditorBlock.Link(id, url) }
        }

        private fun addMediaBlockInternal(createBlock: (Long) -> EditorBlock) {
            _uiState.update { state ->
                val blocks = state.editorBlocks.toMutableList()
                val insertAfterIndex = state.focusedBlockId
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

        private fun getFileName(uri: Uri): String =
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: uri.lastPathSegment ?: "파일"

        private fun save(
            title: String,
            textContents: Map<Long, String>,
            status: TimeLetterStatus,
        ) {
            val state = _uiState.value
            if (state.isSaving) return

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                runCatching {
                    val blocks = buildBlocks(state.editorBlocks, textContents)
                    timeLetterRepository.createTimeLetter(
                        title = title.ifBlank { null },
                        blocks = blocks,
                        sendAt =
                            state.sendAt?.let { date ->
                                "${date}T${state.sendHour.toString().padStart(2, '0')}:${state.sendMinute.toString().padStart(2, '0')}:00"
                            },
                        status = status,
                        receiverIds = state.recipientIds.ifEmpty { null },
                    )
                }.onSuccess {
                    val event =
                        if (status == TimeLetterStatus.DRAFT) {
                            TimeLetterWriteEvent.SavedAsDraft
                        } else {
                            TimeLetterWriteEvent.Registered
                        }
                    _events.send(event)
                    if (status == TimeLetterStatus.DRAFT) loadDraftCount()
                }.onFailure { e ->
                    android.util.Log.e("TimeLetterVM", "save failed", e)
                    _uiState.update { it.copy(errorMessage = "저장에 실패했어요. 다시 시도해주세요.") }
                }
                _uiState.update { it.copy(isSaving = false) }
            }
        }

        private suspend fun buildBlocks(
            editorBlocks: List<EditorBlock>,
            textContents: Map<Long, String>,
        ): List<NewTimeLetterBlock> {
            val blocks = mutableListOf<NewTimeLetterBlock>()
            var order = 1

            for (block in editorBlocks) {
                when (block) {
                    is EditorBlock.Text -> {
                        val content = textContents[block.id] ?: ""
                        if (content.isNotBlank()) {
                            blocks.add(
                                NewTimeLetterBlock(
                                    blockType = TimeLetterBlockType.TEXT,
                                    blockOrder = order++,
                                    textContent = content,
                                ),
                            )
                        }
                    }
                    is EditorBlock.Image -> {
                        val url =
                            photoUploadRepository
                                .upload(block.uri.toString(), "timeletters")
                                .getOrElse { throw it }
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.IMAGE,
                                blockOrder = order++,
                                url = url,
                                mimeType = context.contentResolver.getType(block.uri),
                            ),
                        )
                    }
                    is EditorBlock.Audio -> {
                        val url =
                            photoUploadRepository
                                .upload(block.uri.toString(), "timeletters")
                                .getOrElse { throw it }
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.AUDIO,
                                blockOrder = order++,
                                url = url,
                                mimeType = context.contentResolver.getType(block.uri),
                            ),
                        )
                    }
                    is EditorBlock.File -> {
                        val url =
                            photoUploadRepository
                                .upload(block.uri.toString(), "timeletters")
                                .getOrElse { throw it }
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.FILE,
                                blockOrder = order++,
                                url = url,
                                mimeType = context.contentResolver.getType(block.uri),
                            ),
                        )
                    }
                    is EditorBlock.Link -> {
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.LINK,
                                blockOrder = order++,
                                url = block.url,
                            ),
                        )
                    }
                }
            }

            return blocks
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
