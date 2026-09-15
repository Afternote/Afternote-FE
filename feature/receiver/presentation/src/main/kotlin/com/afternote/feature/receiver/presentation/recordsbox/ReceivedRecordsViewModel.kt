package com.afternote.feature.receiver.presentation.recordsbox

import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 등록된 발신자 카드를 기존 SenderRegistry 계약 그대로 관찰한다. */
@HiltViewModel
internal class ReceivedRecordsViewModel
    @Inject
    constructor(
        senderRegistry: SenderRegistry,
    ) : MviViewModel<ReceivedRecordsIntent, ReceivedRecordsUiState, ReceivedRecordsReducerEvent>(
            ReceivedRecordsUiState(senders = senderRegistry.senders.value),
        ) {
        init {
            viewModelScope.launch {
                senderRegistry.senders.collect { dispatch(ReceivedRecordsReducerEvent.SendersChanged(it)) }
            }
        }

        // 이 화면의 상호작용은 모두 기존 네비게이션 콜백으로 처리한다.
        override fun onIntent(intent: ReceivedRecordsIntent) = Unit

        override fun reduce(
            state: ReceivedRecordsUiState,
            event: ReceivedRecordsReducerEvent,
        ): ReceivedRecordsUiState = reduceReceivedRecords(state, event)
    }
