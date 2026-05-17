package com.afternote.afternote_fe.screen.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.afternote_fe.screen.receiver.model.AfternoteSourceIcon
import com.afternote.afternote_fe.screen.receiver.model.MindRecordSummary
import com.afternote.afternote_fe.screen.receiver.model.ReceiverDownloadState
import com.afternote.afternote_fe.screen.receiver.model.ReceiverHomeUiState
import com.afternote.afternote_fe.screen.receiver.model.SenderMessage
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.model.receiver.AfterNotesListResult
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.shared.util.getAfternoteDisplayRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 홈 화면 ViewModel.
 *
 * 한 마디·각 섹션 카운트·애프터노트 아이콘 목록을 한 번에 모아 단일 [ReceiverHomeUiState]로 노출하고,
 * 모든 기록 내려받기 다이얼로그/요청 상태도 동일 객체에 합친다.
 */
@HiltViewModel
class ReceiverHomeViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ReceiverHomeUiState>(ReceiverHomeUiState.Loading)
        val uiState: StateFlow<ReceiverHomeUiState> = _uiState.asStateFlow()

        init {
            loadHome()
        }

        fun onEvent(event: ReceiverHomeEvent) {
            when (event) {
                ReceiverHomeEvent.Retry -> loadHome()
                ReceiverHomeEvent.RequestDownload -> updateDownload(ReceiverDownloadState.Confirming)
                ReceiverHomeEvent.DismissDownload -> updateDownload(ReceiverDownloadState.Idle)
                ReceiverHomeEvent.ConfirmDownload -> downloadAll()
                ReceiverHomeEvent.ConsumeDownloadResult -> updateDownload(ReceiverDownloadState.Idle)
            }
        }

        private fun loadHome() {
            _uiState.value = ReceiverHomeUiState.Loading
            viewModelScope.launch {
                val afternotes = async { receiverRepository.getReceivedAfterNotes() }
                val mindRecords = async { receiverRepository.loadMindRecordsCount() }
                val timeLetters = async { receiverRepository.loadTimeLettersCount() }
                val message = async { receiverRepository.loadSenderMessage() }
                val afternotesRes = afternotes.await()
                val mindRecordsRes = mindRecords.await()
                val timeLettersRes = timeLetters.await()
                val messageRes = message.await()

                // 모든 호출이 실패한 경우만 Error. 일부 실패는 fallback 으로 진행.
                if (afternotesRes.isFailure &&
                    mindRecordsRes.isFailure &&
                    timeLettersRes.isFailure &&
                    messageRes.isFailure
                ) {
                    _uiState.value =
                        ReceiverHomeUiState.Error(
                            afternotesRes.exceptionOrNull() ?: RuntimeException("All home requests failed"),
                        )
                    return@launch
                }

                val afternotesResult =
                    afternotesRes.getOrNull() ?: AfterNotesListResult(items = emptyList(), totalCount = 0)
                val mindRecordsCount = mindRecordsRes.getOrNull()?.totalCount ?: 0
                val timeLettersCount = timeLettersRes.getOrNull()?.totalCount ?: 0
                val senderMessageInfo = messageRes.getOrNull()
                val senderMessageBody = senderMessageInfo?.message?.takeIf { it.isNotBlank() }

                _uiState.value =
                    ReceiverHomeUiState.Success(
                        senderName = senderMessageInfo?.senderName.orEmpty(),
                        // TODO: 백엔드 응답에 date 필드 추가되면 채울 것. 현재 receiver-auth/message 응답은 senderName/message 만.
                        senderMessage = senderMessageBody?.let { SenderMessage(date = "", body = it) },
                        mindRecord =
                            MindRecordSummary(
                                totalCount = mindRecordsCount,
                                dailyQuestionCount = 0,
                                diaryCount = 0,
                                deepThoughtCount = 0,
                            ),
                        timeLetterTotalCount = timeLettersCount,
                        afternoteTotalCount = afternotesResult.totalCount,
                        afternoteIcons = afternotesResult.items.toAfternoteIcons(),
                    )
            }
        }

        private fun downloadAll() {
            updateDownload(ReceiverDownloadState.InProgress)
            viewModelScope.launch {
                receiverRepository
                    .downloadAllReceived()
                    .onSuccess { bundle ->
                        receiverRepository
                            .saveReceivedExportToFile(bundle)
                            .onSuccess { updateDownload(ReceiverDownloadState.Done) }
                            .onFailure { e ->
                                updateDownload(
                                    ReceiverDownloadState.Failed(e.message ?: "파일 저장에 실패했습니다."),
                                )
                            }
                    }.onFailure { e ->
                        updateDownload(
                            ReceiverDownloadState.Failed(e.message ?: "모든 기록 내려받기에 실패했습니다."),
                        )
                    }
            }
        }

        private fun updateDownload(next: ReceiverDownloadState) {
            _uiState.update { state ->
                if (state is ReceiverHomeUiState.Success) state.copy(download = next) else state
            }
        }
    }

private const val MAX_AFTERNOTE_ICONS = 4

private fun List<AfterNoteListItemDto>.toAfternoteIcons(): List<AfternoteSourceIcon> =
    asSequence()
        .mapNotNull { it.sourceType?.takeIf(String::isNotBlank) }
        .distinct()
        .take(MAX_AFTERNOTE_ICONS)
        .map { typeKey ->
            AfternoteSourceIcon(
                drawableResId = getAfternoteDisplayRes(typeKey).drawableResId,
            )
        }.toList()
