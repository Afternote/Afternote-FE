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
        // receiverListFlow 는 실패를 마지막 목록이나 빈 목록으로 낮춰 실패와 실제 0건이 같은 모양이 된다(#1099).
        // 상태 Flow 는 둘을 가르고(#2045), 세션 교체 시 이전 계정 목록도 SignedOut 으로 먼저 비운다(#2135).
        val uiState: StateFlow<RecipientListUiState> =
            receiverRepository.receiverListStateFlow
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = RecipientListUiState.Loading,
                )

        fun retry() {
            receiverRepository.refreshReceiverList()
        }
    }

private fun ReceiverListState.toUiState(): RecipientListUiState =
    when (this) {
        ReceiverListState.SignedOut -> RecipientListUiState.Success(emptyList())

        // 갱신 로딩은 이 구독이 이미 낸 목록을 그대로 둔다 — 재시도·재조회 중 선택 화면이 깜빡이지 않게.
        is ReceiverListState.Loading -> previousReceivers?.toSuccess() ?: RecipientListUiState.Loading

        is ReceiverListState.Success -> receivers.toSuccess()

        // 이전 목록이 있으면 그 목록을 유지하고, 없거나 401 로 버렸으면 실패를 그대로 보인다(#714).
        is ReceiverListState.Failure ->
            previousReceivers?.takeUnless { discardPrevious }?.toSuccess() ?: RecipientListUiState.Error
    }

private fun List<Receiver>.toSuccess(): RecipientListUiState =
    RecipientListUiState.Success(map { ReceiverListItem(it.receiverId, it.name, it.relation) })
