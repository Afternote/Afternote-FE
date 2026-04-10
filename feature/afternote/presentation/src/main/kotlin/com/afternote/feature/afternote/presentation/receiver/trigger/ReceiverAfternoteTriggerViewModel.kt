package com.afternote.feature.afternote.presentation.receiver.trigger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 HOME에서 콘텐츠 개수(마음의 기록, 타임레터, 애프터노트) 및 leaveMessage를 로드하는 ViewModel.
 */
@HiltViewModel
class ReceiverAfternoteTriggerViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val _leaveMessage = MutableStateFlow<String?>(null)
        val leaveMessage: StateFlow<String?> = _leaveMessage.asStateFlow()

        private val _mindRecordTotalCount = MutableStateFlow(0)
        val mindRecordTotalCount: StateFlow<Int> = _mindRecordTotalCount.asStateFlow()

        private val _timeLetterTotalCount = MutableStateFlow(0)
        val timeLetterTotalCount: StateFlow<Int> = _timeLetterTotalCount.asStateFlow()

        private val _afternoteTotalCount = MutableStateFlow(0)
        val afternoteTotalCount: StateFlow<Int> = _afternoteTotalCount.asStateFlow()

        fun onEvent(event: ReceiverAfternoteTriggerEvent) {
            when (event) {
                is ReceiverAfternoteTriggerEvent.LoadHomeSummary -> loadHomeSummary(event.authCode)
                is ReceiverAfternoteTriggerEvent.LoadAfterNotes -> loadAfterNotes(event.authCode)
            }
        }

        private fun loadHomeSummary(authCode: String) {
            viewModelScope.launch {
                val afternotesDeferred = async { receiverRepository.getAfterNotesByAuthCode(authCode) }
                val mindRecordsDeferred = async { receiverRepository.loadMindRecordsCount(authCode) }
                val timeLettersDeferred = async { receiverRepository.loadTimeLettersCount(authCode) }
                val messageDeferred = async { receiverRepository.loadSenderMessage(authCode) }
                awaitAll(
                    afternotesDeferred,
                    mindRecordsDeferred,
                    timeLettersDeferred,
                    messageDeferred,
                )

                messageDeferred.await().onSuccess { message ->
                    _leaveMessage.value = message?.takeIf { it.isNotBlank() }
                }
                afternotesDeferred.await().onSuccess { result ->
                    _afternoteTotalCount.value = result.totalCount
                }
                mindRecordsDeferred.await().onSuccess { result ->
                    _mindRecordTotalCount.value = result.totalCount
                }
                timeLettersDeferred.await().onSuccess { result ->
                    _timeLetterTotalCount.value = result.totalCount
                }
            }
        }

        private fun loadAfterNotes(authCode: String) {
            viewModelScope.launch {
                receiverRepository
                    .getAfterNotesByAuthCode(authCode)
                    .onSuccess { result ->
                        _afternoteTotalCount.value = result.totalCount
                    }
            }
        }
    }
