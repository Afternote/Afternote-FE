package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.Receiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface RecipientListUiState {
    data object Loading : RecipientListUiState

    data class Success(
        val recipients: List<ReceiverListItem>,
    ) : RecipientListUiState

    data object Error : RecipientListUiState
}

@HiltViewModel
class RecipientListViewModel
    @Inject
    constructor(
        private val receiverRepository: UserReceiverRepository,
    ) : ViewModel() {
        // 마지막으로 보여 준 목록과 그 세션. 저장소의 이전 목록은 구독 단위라 5초 넘게 끊겼다 다시 구독하면
        // null 로 시작한다 — 그 첫 조회가 잠깐 실패해도 떠 있던 목록을 지키려고 ViewModel 이 들고 있다가
        // SignedOut·401 에서만 버린다. 설정 수신자 목록(#2045)과 같은 방식이다.
        private var shownReceivers: List<Receiver>? = null
        private var shownSessionId: String? = null

        // receiverListFlow 는 실패를 마지막 목록이나 빈 목록으로 낮춰 실패와 실제 0건이 같은 모양이 된다(#1099).
        // 상태 Flow 는 둘을 가르고(#2045), 세션 교체 시 이전 계정 목록도 SignedOut 으로 먼저 비운다(#2135).
        val uiState: StateFlow<RecipientListUiState> =
            receiverRepository.receiverListStateFlow
                .map(::reduce)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = RecipientListUiState.Loading,
                )

        fun retry() {
            receiverRepository.refreshReceiverList()
        }

        private fun reduce(state: ReceiverListState): RecipientListUiState =
            when (state) {
                ReceiverListState.SignedOut -> {
                    shownReceivers = null
                    shownSessionId = null
                    RecipientListUiState.Success(emptyList())
                }

                is ReceiverListState.Loading -> {
                    // 다른 세션에서 보여 준 목록은 이어받지 않는다.
                    if (shownSessionId != state.sessionId) shownReceivers = null
                    shownSessionId = state.sessionId
                    val rows = state.previousReceivers ?: shownReceivers
                    rows?.toSuccess() ?: RecipientListUiState.Loading
                }

                is ReceiverListState.Success -> {
                    shownReceivers = state.receivers
                    state.receivers.toSuccess()
                }

                // 보여 줄 목록이 있으면 유지하고, 없거나 401 로 버렸으면 실패를 그대로 보인다(#714).
                is ReceiverListState.Failure -> {
                    if (state.discardPrevious) shownReceivers = null
                    val rows = if (state.discardPrevious) null else state.previousReceivers ?: shownReceivers
                    rows?.toSuccess() ?: RecipientListUiState.Error
                }
            }
    }

private fun List<Receiver>.toSuccess(): RecipientListUiState =
    RecipientListUiState.Success(map { ReceiverListItem(it.receiverId, it.name, it.relation) })
