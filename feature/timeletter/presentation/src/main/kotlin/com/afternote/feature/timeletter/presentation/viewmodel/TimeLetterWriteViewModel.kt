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
            content: String,
        ) {
            save(title = title, content = content, status = TimeLetterStatus.DRAFT)
        }

        fun register(
            title: String,
            content: String,
        ) {
            if (_uiState.value.sendAt == null) {
                _uiState.update { it.copy(errorMessage = "발송 날짜를 선택해주세요.") }
                return
            }
            save(title = title, content = content, status = TimeLetterStatus.SCHEDULED)
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun setTextAlign(align: TextAlign) {
            _uiState.update { it.copy(textAlign = align) }
        }

        fun addImageAttachment(uri: Uri) {
            _uiState.update {
                it.copy(attachments = it.attachments + LetterAttachment.ImageAttachment(uri, getFileName(uri)))
            }
        }

        fun addAudioAttachment(uri: Uri) {
            _uiState.update {
                it.copy(attachments = it.attachments + LetterAttachment.AudioAttachment(uri, getFileName(uri)))
            }
        }

        fun addFileAttachment(uri: Uri) {
            _uiState.update {
                it.copy(attachments = it.attachments + LetterAttachment.FileAttachment(uri, getFileName(uri)))
            }
        }

        fun addLinkAttachment(url: String) {
            _uiState.update {
                it.copy(attachments = it.attachments + LetterAttachment.LinkAttachment(url))
            }
        }

        fun removeAttachment(index: Int) {
            _uiState.update {
                it.copy(attachments = it.attachments.toMutableList().apply { removeAt(index) })
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
            content: String,
            status: TimeLetterStatus,
        ) {
            val state = _uiState.value
            if (state.isSaving) return

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                runCatching {
                    android.util.Log.d("TimeLetterVM", "save: status=$status, sendAt=${state.sendAt}, sendHour=${state.sendHour}, sendMinute=${state.sendMinute}, attachments=${state.attachments.size}, receiverIds=${state.recipientIds}")
                    val blocks = buildBlocks(content, state.attachments)
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
                    android.util.Log.d("TimeLetterVM", "save success: status=$status")
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
            content: String,
            attachments: List<LetterAttachment>,
        ): List<NewTimeLetterBlock> {
            val blocks = mutableListOf<NewTimeLetterBlock>()
            var order = 1

            if (content.isNotBlank()) {
                blocks.add(
                    NewTimeLetterBlock(
                        blockType = TimeLetterBlockType.TEXT,
                        blockOrder = order++,
                        textContent = content,
                    ),
                )
            }

            for (attachment in attachments) {
                when (attachment) {
                    is LetterAttachment.ImageAttachment -> {
                        val url =
                            photoUploadRepository
                                .upload(attachment.uri.toString(), "timeletters")
                                .getOrElse { throw it }
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.IMAGE,
                                blockOrder = order++,
                                url = url,
                                mimeType = context.contentResolver.getType(attachment.uri),
                            ),
                        )
                    }
                    is LetterAttachment.AudioAttachment -> {
                        val url =
                            photoUploadRepository
                                .upload(attachment.uri.toString(), "timeletters")
                                .getOrElse { throw it }
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.AUDIO,
                                blockOrder = order++,
                                url = url,
                                mimeType = context.contentResolver.getType(attachment.uri),
                            ),
                        )
                    }
                    is LetterAttachment.FileAttachment -> {
                        val url =
                            photoUploadRepository
                                .upload(attachment.uri.toString(), "timeletters")
                                .getOrElse { throw it }
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.FILE,
                                blockOrder = order++,
                                url = url,
                                mimeType = context.contentResolver.getType(attachment.uri),
                            ),
                        )
                    }
                    is LetterAttachment.LinkAttachment -> {
                        blocks.add(
                            NewTimeLetterBlock(
                                blockType = TimeLetterBlockType.LINK,
                                blockOrder = order++,
                                url = attachment.url,
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
