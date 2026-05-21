package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.data.cache.ReceiverCacheStore
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
            save(title = title, content = content, status = TimeLetterStatus.SCHEDULED)
        }

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
                    timeLetterRepository.createTimeLetter(
                        title = title.ifBlank { null },
                        content = content.ifBlank { null },
                        sendAt = state.sendAt,
                        status = status,
                        mediaList = null,
                        receiverIds = state.recipientIds.ifEmpty { null },
                        deliveredAt = null,
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
                }.onFailure {
                    _events.send(TimeLetterWriteEvent.Error("저장에 실패했어요. 다시 시도해주세요."))
                }
                _uiState.update { it.copy(isSaving = false) }
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

        fun setTextAlign(align: TextAlign) {
            _uiState.update { it.copy(textAlign = align) }
        }
    }
