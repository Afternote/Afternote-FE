package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordBox
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 받은 기록함 화면 ViewModel — 등록된 발신자 카드 리스트 노출 (이슈 #215).
 *
 * 저장된 접근 코드가 있으면 [ReceiverRepository.getReceivedRecordBoxes]를 호출해 서버 목록을 표시하며,
 * 앱 재시작 뒤에도 DataStore의 접근 코드로 목록을 복원한다. 접근 코드가 없으면 이전 목록을 제거한다.
 */
@HiltViewModel
class ReceivedRecordsViewModel
    @Inject
    constructor(
        private val receivedRecordStore: ReceivedRecordStore,
        private val receiverRepository: ReceiverRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val loadState = MutableStateFlow(LoadState.Loading)
        private var activeAuthCode: String? = null

        val uiState: StateFlow<ReceivedRecordsUiState> =
            combine(receivedRecordStore.recordBoxes, loadState) { recordBoxes, state ->
                ReceivedRecordsUiState(
                    senders = recordBoxes,
                    isLoading = state == LoadState.Loading,
                    hasLoadError = state == LoadState.Failed,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ReceivedRecordsUiState(),
            )

        init {
            viewModelScope.launch {
                receiverRepository.authCodeFlow
                    .distinctUntilChanged()
                    .collectLatest { authCode ->
                        activeAuthCode = authCode
                        if (authCode == null) {
                            receivedRecordStore.clear()
                            loadState.value = LoadState.Ready
                        } else {
                            loadState.value = LoadState.Loading
                            receivedRecordStore.clear()
                            loadRecordBoxes(expectedAuthCode = authCode)
                        }
                    }
            }
        }

        fun retry() {
            val authCode = activeAuthCode ?: return
            if (loadState.value == LoadState.Loading) return
            viewModelScope.launch { loadRecordBoxes(expectedAuthCode = authCode) }
        }

        private suspend fun loadRecordBoxes(expectedAuthCode: String) {
            loadState.value = LoadState.Loading
            receiverRepository
                .getReceivedRecordBoxes()
                .onSuccess { recordBoxes ->
                    if (activeAuthCode != expectedAuthCode) return@onSuccess
                    try {
                        receivedRecordStore.replaceRecordBoxes(recordBoxes.map(ReceivedRecordBox::toReceivedRecordItem))
                        loadState.value = LoadState.Ready
                    } catch (throwable: DuplicateRecordBoxIdException) {
                        handleLoadFailure(throwable)
                    }
                }.onFailure { throwable ->
                    if (activeAuthCode != expectedAuthCode) return@onFailure
                    handleLoadFailure(throwable)
                }
        }

        private fun handleLoadFailure(throwable: Throwable) {
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.RECEIVED_RECORD_BOXES_LOAD, throwable)
            loadState.value = LoadState.Failed
        }

        private enum class LoadState {
            Loading,
            Ready,
            Failed,
        }
    }

private fun ReceivedRecordBox.toReceivedRecordItem(): ReceivedRecordItem =
    ReceivedRecordItem(
        recordBoxId = recordBoxId,
        accessCode = accessCode,
        senderName = senderName,
        receiverName = receiverName,
        relation = relation,
        recordStatus = recordStatus,
        viewStatus = viewStatus,
        verification = verification,
    )
