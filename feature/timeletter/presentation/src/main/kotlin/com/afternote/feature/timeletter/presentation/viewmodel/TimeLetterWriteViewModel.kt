package com.afternote.feature.timeletter.presentation.viewmodel

import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
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
        private val editingTimeLetterId =
            savedStateHandle.toRoute<TimeLetterRoute.TimeLetterWriteRoute>().timeLetterId

        private val _uiState =
            MutableStateFlow(
                TimeLetterWriteUiState(
                    isLoadingEditingLetter = editingTimeLetterId != null,
                ),
            )
        val uiState: StateFlow<TimeLetterWriteUiState> = _uiState.asStateFlow()

        private var receiverNameMap: Map<Long, String> = emptyMap()

        init {
            viewModelScope.launch {
                receiverNameMap =
                    runCatching { userRepository.getReceivers() }
                        .getOrElse { emptyList() }
                        .associate { it.receiverId to it.name }
                editingTimeLetterId?.let { loadEditingTimeLetter(it) }
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
                val uriString = uri.toString()
                val name = fileMetadataRepository.getFileName(uriString)
                val mimeType = fileMetadataRepository.getMimeType(uriString)
                addMediaBlockInternal { id -> EditorBlock.Image(id, uri, name, mimeType) }
            }
        }

        fun addAudioBlock(uri: Uri) {
            viewModelScope.launch {
                val uriString = uri.toString()
                val name = fileMetadataRepository.getFileName(uriString)
                val mimeType = fileMetadataRepository.getMimeType(uriString)
                addMediaBlockInternal { id -> EditorBlock.Audio(id, uri, name, mimeType) }
            }
        }

        fun addFileBlock(uri: Uri) {
            viewModelScope.launch {
                val uriString = uri.toString()
                val name = fileMetadataRepository.getFileName(uriString)
                val mimeType = fileMetadataRepository.getMimeType(uriString)
                addMediaBlockInternal { id -> EditorBlock.File(id, uri, name, mimeType) }
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
                        ?.let { focusedId ->
                            blocks.indexOfFirst { it.id == focusedId }.takeIf { it >= 0 }
                        }
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
                val sendAt =
                    state.sendAt?.let { date ->
                        "${date}T${
                            state.sendHour.toString().padStart(2, '0')
                        }:${state.sendMinute.toString().padStart(2, '0')}:00"
                    }
                val saveResult =
                    if (state.editingTimeLetterId == null) {
                        val blocks = mapToBlockInputs(state.editorBlocks, textContents)
                        createTimeLetterUseCase(
                            title = title.ifBlank { null },
                            blocks = blocks,
                            sendAt = sendAt,
                            status = status,
                            receiverIds = state.recipientIds.ifEmpty { null },
                        )
                    } else {
                        runCatching {
                            timeLetterRepository.updateTimeLetter(
                                timeLetterId = state.editingTimeLetterId,
                                title = title.ifBlank { null },
                                blocks = mapToUpdateBlocks(state.editorBlocks, textContents),
                                sendAt = sendAt,
                                status = status,
                            )
                        }
                    }
                saveResult
                    .onSuccess {
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

        private suspend fun loadEditingTimeLetter(timeLetterId: Long) {
            runCatching { timeLetterRepository.getTimeLetter(timeLetterId) }
                .onSuccess { letter ->
                    val editorBlocks = letter.toEditorBlocks()
                    val sendAtDate = letter.sendAt?.take(10)
                    val sendHour =
                        letter.sendAt
                            ?.substringAfter("T")
                            ?.take(2)
                            ?.toIntOrNull()
                            ?: 0
                    val sendMinute =
                        letter.sendAt
                            ?.substringAfter("T")
                            ?.drop(3)
                            ?.take(2)
                            ?.toIntOrNull()
                            ?: 0

                    _uiState.update {
                        it.copy(
                            editingTimeLetterId = letter.id,
                            isLoadingEditingLetter = false,
                            initialTitle = letter.title.orEmpty(),
                            initialTextContents =
                                letter.blocks
                                    .filter { block -> block.blockType == TimeLetterBlockType.TEXT }
                                    .associate { block -> block.id to (block.textContent ?: "") },
                            recipientIds = letter.receiverIds,
                            recipientNames = letter.receiverIds.mapNotNull { id -> receiverNameMap[id] },
                            sendAt = sendAtDate,
                            sendTime = formatDisplayTime(sendHour, sendMinute),
                            sendHour = sendHour,
                            sendMinute = sendMinute,
                            editorBlocks = editorBlocks,
                            focusedBlockId = editorBlocks.firstOrNull()?.id,
                            nextBlockId = (editorBlocks.maxOfOrNull { block -> block.id } ?: 0L) + 1L,
                        )
                    }
                }.onFailure {
                    _uiState.update { it.copy(isLoadingEditingLetter = false) }
                    _uiState.update { it.copy(errorMessage = "타임레터를 불러올 수 없습니다.") }
                }
        }

        private fun TimeLetter.toEditorBlocks(): List<EditorBlock> {
            val blocks =
                this.blocks
                    .sortedBy { it.blockOrder }
                    .mapNotNull { block ->
                        when (block.blockType) {
                            TimeLetterBlockType.TEXT -> {
                                EditorBlock.Text(id = block.id)
                            }

                            TimeLetterBlockType.LINK -> {
                                block.url?.let { EditorBlock.Link(id = block.id, url = it) }
                            }

                            TimeLetterBlockType.IMAGE -> {
                                block.url?.let {
                                    EditorBlock.Image(
                                        id = block.id,
                                        uri = Uri.parse(it),
                                        name = it.fileNameOrFallback("image"),
                                        mimeType = block.mimeType,
                                    )
                                }
                            }

                            TimeLetterBlockType.AUDIO -> {
                                block.url?.let {
                                    EditorBlock.Audio(
                                        id = block.id,
                                        uri = Uri.parse(it),
                                        name = it.fileNameOrFallback("audio"),
                                        mimeType = block.mimeType,
                                    )
                                }
                            }

                            TimeLetterBlockType.FILE -> {
                                block.url?.let {
                                    EditorBlock.File(
                                        id = block.id,
                                        uri = Uri.parse(it),
                                        name = it.fileNameOrFallback("file"),
                                        mimeType = block.mimeType,
                                    )
                                }
                            }
                        }
                    }
            return blocks.ifEmpty { listOf(EditorBlock.Text(id = 0L)) }
        }

        private fun String.fileNameOrFallback(fallback: String): String =
            substringBefore('?')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
                ?: fallback

        private fun formatDisplayTime(
            hour: Int,
            minute: Int,
        ): String {
            val amPm = if (hour < 12) "오전" else "오후"
            val displayHour =
                when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
            return "$amPm $displayHour:${minute.toString().padStart(2, '0')}"
        }

        private fun mapToUpdateBlocks(
            editorBlocks: List<EditorBlock>,
            textContents: Map<Long, String>,
        ): List<NewTimeLetterBlock> =
            buildList {
                var order = 1
                for (block in editorBlocks) {
                    when (block) {
                        is EditorBlock.Text -> {
                            val content = textContents[block.id] ?: ""
                            if (content.isNotBlank()) {
                                add(
                                    NewTimeLetterBlock(
                                        blockType = TimeLetterBlockType.TEXT,
                                        blockOrder = order++,
                                        textContent = content,
                                    ),
                                )
                            }
                        }

                        is EditorBlock.Link -> {
                            add(
                                NewTimeLetterBlock(
                                    blockType = TimeLetterBlockType.LINK,
                                    blockOrder = order++,
                                    url = block.url,
                                ),
                            )
                        }

                        is EditorBlock.Image -> {
                            add(
                                NewTimeLetterBlock(
                                    blockType = TimeLetterBlockType.IMAGE,
                                    blockOrder = order++,
                                    url = block.uri.toString(),
                                    mimeType = block.mimeType,
                                ),
                            )
                        }

                        is EditorBlock.Audio -> {
                            add(
                                NewTimeLetterBlock(
                                    blockType = TimeLetterBlockType.AUDIO,
                                    blockOrder = order++,
                                    url = block.uri.toString(),
                                    mimeType = block.mimeType,
                                ),
                            )
                        }

                        is EditorBlock.File -> {
                            add(
                                NewTimeLetterBlock(
                                    blockType = TimeLetterBlockType.FILE,
                                    blockOrder = order++,
                                    url = block.uri.toString(),
                                    mimeType = block.mimeType,
                                ),
                            )
                        }
                    }
                }
            }

        private suspend fun mapToBlockInputs(
            editorBlocks: List<EditorBlock>,
            textContents: Map<Long, String>,
        ): List<BlockInput> =
            buildList {
                for (block in editorBlocks) {
                    when (block) {
                        is EditorBlock.Text -> {
                            val content = textContents[block.id] ?: ""
                            if (content.isNotBlank()) add(BlockInput.Text(content))
                        }

                        is EditorBlock.Image -> {
                            add(
                                BlockInput.Media(
                                    uriString = block.uri.toString(),
                                    mimeType = fileMetadataRepository.getMimeType(block.uri.toString()),
                                    blockType = TimeLetterBlockType.IMAGE,
                                ),
                            )
                        }

                        is EditorBlock.Audio -> {
                            add(
                                BlockInput.Media(
                                    uriString = block.uri.toString(),
                                    mimeType = fileMetadataRepository.getMimeType(block.uri.toString()),
                                    blockType = TimeLetterBlockType.AUDIO,
                                ),
                            )
                        }

                        is EditorBlock.File -> {
                            add(
                                BlockInput.Media(
                                    uriString = block.uri.toString(),
                                    mimeType = fileMetadataRepository.getMimeType(block.uri.toString()),
                                    blockType = TimeLetterBlockType.FILE,
                                ),
                            )
                        }

                        is EditorBlock.Link -> {
                            add(BlockInput.Link(block.url))
                        }
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
