package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 받은 기록함 화면 ViewModel — 등록된 발신자 카드 리스트 노출 (이슈 #215).
 *
 * 현재는 [SenderRegistry] 의 in-memory 데이터를 그대로 흘려보낸다. 백엔드 *발신자 리스트 조회 API*
 * 가 확정되면 Repository 호출로 교체한다.
 */
@HiltViewModel
class ReceivedRecordsViewModel
    @Inject
    constructor(
        senderRegistry: SenderRegistry,
    ) : ViewModel() {
        val senders: StateFlow<List<SenderEntry>> = senderRegistry.senders
    }
