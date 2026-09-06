package com.afternote.feature.receiver.presentation.recordsbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 받은 기록함 화면 ViewModel — 등록된 발신자 카드 리스트 노출 (이슈 #215).
 *
 * [SenderRegistry] 의 영속 목록 Flow 를 화면 수명 [StateFlow]로 바꿔 노출한다.
 */
@HiltViewModel
class ReceivedRecordsViewModel
    @Inject
    constructor(
        senderRegistry: SenderRegistry,
    ) : ViewModel() {
        val senders: StateFlow<List<SenderEntry>> =
            senderRegistry.senders.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
    }
