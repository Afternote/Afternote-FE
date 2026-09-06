package com.afternote.feature.timeletter.presentation.viewmodel

import android.net.Uri
import android.os.SystemClock
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.error.TimeLetterServerRejectionException
import com.afternote.feature.timeletter.domain.model.BlockInput
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterBlockType
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.domain.repository.VoiceRecorderRepository
import com.afternote.feature.timeletter.domain.usecase.CreateTimeLetterUseCase
import com.afternote.feature.timeletter.domain.usecase.ResolveTimeLetterBlocksUseCase
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        private val resolveTimeLetterBlocksUseCase: ResolveTimeLetterBlocksUseCase,
        private val timeLetterRepository: TimeLetterRepository,
        private val userRepository: UserRepository,
        private val fileMetadataRepository: FileMetadataRepository,
        private val voiceRecorderRepository: VoiceRecorderRepository,
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
        private var recordingTimerJob: Job? = null
        private var isCheckingRegisterLimit: Boolean = false
        private var originalEditingStatus: TimeLetterStatus? = null

        /**
         * 화면이 닫힌 뒤 도착하는 start()/stop() 완료를 걸러낸다. discard/retry/openVoiceRecorder
         * 로 현재 녹음 시도를 포기할 때마다 증가시켜, 그 이전에 시작된 start()/stop() 이 나중에
         * 성공하더라도 UI 를 되살리지 않고 즉시 파일을 정리하게 한다 (#440 리뷰).
         */
        private var recorderGeneration = 0

        init {
            viewModelScope.launch {
                receiverNameMap =
                    runCatchingCancellable { userRepository.getReceivers() }
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

        fun updateDraftTitle(title: String) {
            _uiState.update { state -> state.copy(draftTitle = title) }
        }

        fun updateDraftContent(
            title: String,
            textContents: Map<Long, String>,
        ) {
            _uiState.update { state ->
                state.copy(
                    draftTitle = title,
                    draftTextContents = state.draftTextContents + textContents,
                )
            }
        }

        fun updateDraftTextContent(
            blockId: Long,
            content: String,
        ) {
            _uiState.update { state ->
                state.copy(
                    draftTextContents = state.draftTextContents + (blockId to content),
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
            val state = _uiState.value
            if (state.isSaving || isCheckingRegisterLimit) return

            if (state.recipientIds.isEmpty()) {
                _uiState.update { it.copy(error = TimeLetterWriteError.RecipientRequired) }
                return
            }
            if (state.sendAt == null) {
                _uiState.update { it.copy(error = TimeLetterWriteError.SendDateRequired) }
                return
            }
            isCheckingRegisterLimit = true
            viewModelScope.launch {
                try {
                    if (originalEditingStatus != TimeLetterStatus.SCHEDULED) {
                        val registeredCount =
                            runCatchingCancellable { timeLetterRepository.getTimeLetters().totalCount }
                                .getOrElse {
                                    _uiState.update { current ->
                                        current.copy(error = TimeLetterWriteError.LoadFailed)
                                    }
                                    return@launch
                                }

                        if (registeredCount >= FREE_PLAN_REGISTER_LIMIT) {
                            _uiState.update { it.copy(showFreePlanLimitPopup = true) }
                            return@launch
                        }
                    }

                    save(title = title, textContents = textContents, status = TimeLetterStatus.SCHEDULED)
                } finally {
                    isCheckingRegisterLimit = false
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun onSavedAsDraftShown() {
            _uiState.update { it.copy(savedAsDraft = false) }
        }

        fun onRegisteredShown() {
            _uiState.update { it.copy(registered = false) }
        }

        fun dismissFreePlanLimitPopup() {
            _uiState.update { it.copy(showFreePlanLimitPopup = false) }
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

        fun openVoiceRecorder() {
            recorderGeneration++
            _uiState.update {
                it.copy(
                    showVoiceRecorder = true,
                    voiceRecordingState = VoiceRecordingState.Idle,
                )
            }
        }

        fun startVoiceRecording() {
            if (_uiState.value.voiceRecordingState !is VoiceRecordingState.Idle) return
            val generation = recorderGeneration
            _uiState.update { it.copy(voiceRecordingState = VoiceRecordingState.Starting) }
            viewModelScope.launch {
                voiceRecorderRepository
                    .start()
                    .onSuccess {
                        if (generation == recorderGeneration) {
                            _uiState.update { state ->
                                state.copy(voiceRecordingState = VoiceRecordingState.Recording(0L))
                            }
                            startRecordingTimer()
                        } else {
                            // 화면이 이미 닫혀 이 시도를 포기한 뒤 뒤늦게 성공한 경우다.
                            // UI 는 되살리지 않고 방금 시작된 녹음을 바로 회수한다.
                            voiceRecorderRepository.discard()
                        }
                    }.onFailure {
                        if (generation == recorderGeneration) {
                            _uiState.update { state ->
                                state.copy(
                                    voiceRecordingState = VoiceRecordingState.Idle,
                                    error = TimeLetterWriteError.VoiceRecordingStartFailed,
                                )
                            }
                        }
                    }
            }
        }

        fun stopVoiceRecording() {
            if (_uiState.value.voiceRecordingState !is VoiceRecordingState.Recording) return
            val generation = recorderGeneration
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            _uiState.update { it.copy(voiceRecordingState = VoiceRecordingState.Stopping) }
            viewModelScope.launch {
                voiceRecorderRepository
                    .stop()
                    .onSuccess { audio ->
                        if (generation == recorderGeneration) {
                            _uiState.update { state ->
                                state.copy(voiceRecordingState = VoiceRecordingState.Recorded(audio))
                            }
                        } else {
                            voiceRecorderRepository.deleteRecordedFile(audio.uriString)
                        }
                    }.onFailure {
                        if (generation == recorderGeneration) {
                            _uiState.update { state ->
                                state.copy(
                                    voiceRecordingState = VoiceRecordingState.Idle,
                                    error = TimeLetterWriteError.VoiceRecordingStopFailed,
                                )
                            }
                        }
                    }
            }
        }

        fun registerVoiceRecording() {
            val recorded = _uiState.value.voiceRecordingState as? VoiceRecordingState.Recorded ?: return
            val audio = recorded.audio
            voiceRecorderRepository.retainRecordedFile()
            addMediaBlockInternal { id ->
                EditorBlock.Audio(
                    id = id,
                    uri = Uri.parse(audio.uriString),
                    name = audio.fileName,
                    mimeType = audio.mimeType,
                )
            }
            _uiState.update {
                it.copy(
                    showVoiceRecorder = false,
                    voiceRecordingState = VoiceRecordingState.Idle,
                )
            }
        }

        fun discardVoiceRecording() {
            recorderGeneration++
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            viewModelScope.launch {
                voiceRecorderRepository.discard()
                _uiState.update {
                    it.copy(
                        showVoiceRecorder = false,
                        voiceRecordingState = VoiceRecordingState.Idle,
                    )
                }
            }
        }

        fun retryVoiceRecording() {
            recorderGeneration++
            recordingTimerJob?.cancel()
            recordingTimerJob = null
            viewModelScope.launch {
                voiceRecorderRepository.discard()
                _uiState.update { it.copy(voiceRecordingState = VoiceRecordingState.Idle) }
            }
        }

        private fun startRecordingTimer() {
            recordingTimerJob?.cancel()
            recordingTimerJob =
                viewModelScope.launch {
                    val startedAtMillis = SystemClock.elapsedRealtime()
                    while (_uiState.value.voiceRecordingState is VoiceRecordingState.Recording) {
                        delay(RECORDING_TIMER_INTERVAL_MILLIS)
                        val elapsedMillis = SystemClock.elapsedRealtime() - startedAtMillis
                        _uiState.update { state ->
                            if (state.voiceRecordingState is VoiceRecordingState.Recording) {
                                state.copy(voiceRecordingState = VoiceRecordingState.Recording(elapsedMillis))
                            } else {
                                state
                            }
                        }
                    }
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
            val removedAudioUri =
                (_uiState.value.editorBlocks.firstOrNull { it.id == id } as? EditorBlock.Audio)
                    ?.uri
                    ?.toString()
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
            removedAudioUri?.let { uri ->
                viewModelScope.launch { voiceRecorderRepository.deleteRecordedFile(uri) }
            }
        }

        private fun save(
            title: String,
            textContents: Map<Long, String>,
            status: TimeLetterStatus,
        ) {
            val state = _uiState.value
            if (state.isSaving) return
            if (state.recipientIds.isEmpty()) {
                _uiState.update { it.copy(error = TimeLetterWriteError.RecipientRequired) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                try {
                    val sendAt =
                        state.sendAt?.let { date ->
                            formatSendAt(
                                date = date,
                                hour = state.sendHour,
                                minute = state.sendMinute,
                            )
                        }
                    val saveResult =
                        if (state.editingTimeLetterId == null) {
                            val blocks = mapToBlockInputs(state.editorBlocks, textContents)
                            createTimeLetterUseCase(
                                title = title.ifBlank { null },
                                blocks = blocks,
                                sendAt = sendAt,
                                deliveryMode = TimeLetterDeliveryMode.DATE,
                                status = status,
                                receiverIds = state.recipientIds,
                            )
                        } else {
                            runCatchingCancellable {
                                timeLetterRepository.updateTimeLetter(
                                    timeLetterId = state.editingTimeLetterId,
                                    title = title.ifBlank { null },
                                    blocks =
                                        resolveTimeLetterBlocksUseCase(
                                            mapToBlockInputs(state.editorBlocks, textContents),
                                        ),
                                    sendAt = sendAt,
                                    deliveryMode = TimeLetterDeliveryMode.DATE,
                                    status = status,
                                )
                            }
                        }
                    saveResult
                        .onSuccess {
                            state.editorBlocks
                                .filterIsInstance<EditorBlock.Audio>()
                                .forEach { voiceRecorderRepository.deleteRecordedFile(it.uri.toString()) }
                            if (status == TimeLetterStatus.DRAFT) {
                                loadDraftCount()
                                _uiState.update { it.copy(savedAsDraft = true) }
                            } else {
                                _uiState.update { it.copy(registered = true) }
                            }
                        }.onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    error =
                                        if (error is TimeLetterServerRejectionException) {
                                            TimeLetterWriteError.ServerRejection
                                        } else {
                                            TimeLetterWriteError.SaveFailed
                                        },
                                )
                            }
                        }
                } finally {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }

        private suspend fun loadEditingTimeLetter(timeLetterId: Long) {
            runCatchingCancellable { timeLetterRepository.getTimeLetter(timeLetterId) }
                .onSuccess { letter ->
                    originalEditingStatus = letter.status
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
                            draftTitle = letter.title.orEmpty(),
                            draftTextContents =
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
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoadingEditingLetter = false) }
                    _uiState.update {
                        it.copy(
                            error =
                                if (error is TimeLetterServerRejectionException) {
                                    TimeLetterWriteError.ServerRejection
                                } else {
                                    TimeLetterWriteError.LoadFailed
                                },
                        )
                    }
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
                                    mimeType =
                                        block.mimeType
                                            ?: fileMetadataRepository.getMimeType(block.uri.toString()),
                                    blockType = TimeLetterBlockType.IMAGE,
                                ),
                            )
                        }

                        is EditorBlock.Audio -> {
                            add(
                                BlockInput.Media(
                                    uriString = block.uri.toString(),
                                    mimeType =
                                        block.mimeType
                                            ?: fileMetadataRepository.getMimeType(block.uri.toString()),
                                    blockType = TimeLetterBlockType.AUDIO,
                                ),
                            )
                        }

                        is EditorBlock.File -> {
                            add(
                                BlockInput.Media(
                                    uriString = block.uri.toString(),
                                    mimeType =
                                        block.mimeType
                                            ?: fileMetadataRepository.getMimeType(block.uri.toString()),
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
                runCatchingCancellable { timeLetterRepository.getTemporaryTimeLetters() }
                    .onSuccess { result ->
                        _uiState.update { it.copy(draftCount = result.totalCount) }
                    }
            }
        }

        override fun onCleared() {
            recordingTimerJob?.cancel()
            voiceRecorderRepository.release()
            super.onCleared()
        }

        private companion object {
            const val RECORDING_TIMER_INTERVAL_MILLIS = 1_000L
            const val FREE_PLAN_REGISTER_LIMIT = 3
        }
    }

private fun formatSendAt(
    date: String,
    hour: Int,
    minute: Int,
): String = "${date}T${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:00"
