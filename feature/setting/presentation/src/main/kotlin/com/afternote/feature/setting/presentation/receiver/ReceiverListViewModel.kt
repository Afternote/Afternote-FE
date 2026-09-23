package com.afternote.feature.setting.presentation.receiver

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

@HiltViewModel
class ReceiverListViewModel
    @Inject
    constructor(
        private val userReceiverRepository: UserReceiverRepository,
    ) : ViewModel() {
        // 지금 화면에 그린 행. 5초를 넘겨 돌아오면 저장소 구독이 새로 시작해 이전 목록 없는 Loading 부터
        // 오는데, 그 첫 조회 동안과 그 조회가 실패했을 때 이 행을 그대로 둬 검색·선택·스크롤을 지킨다 (#1281).
        // SignedOut, 저장소가 401 로 목록을 버린 실패, 보여 줄 행 없는 실패에서만 비운다. 이 ViewModel 의
        // 수집자는 stateIn 하나뿐이라 두 필드가 동시에 쓰이지 않는다.
        private var shownReceivers: List<ReceiverListItem> = emptyList()

        // 저장소가 직전 상태에 목록을 실었는지. 목록을 싣던 구독이 실패에서 null 을 내면 401 로 버린 것이고,
        // 새 구독은 Loading(null) 부터 내므로 false 로 돌아간다. 이 값으로 두 null 을 가른다.
        private var repositoryCarriedReceivers = false

        val uiState: StateFlow<ReceiverListUiState> =
            userReceiverRepository.receiverListStateFlow
                .map(::toUiState)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ReceiverListUiState(),
                )

        /** 실패 상태에서만 다시 조회한다. 조회 중에는 이미 요청이 나가 있어 무시한다. */
        fun retry() {
            when (uiState.value.loadState) {
                ReceiverListLoadState.Failure, ReceiverListLoadState.RefreshFailure -> {
                    userReceiverRepository.refreshReceiverList()
                }

                ReceiverListLoadState.Loading, ReceiverListLoadState.Ready -> {
                    Unit
                }
            }
        }

        private fun toUiState(state: ReceiverListState): ReceiverListUiState {
            val uiState =
                when (state) {
                    ReceiverListState.SignedOut -> {
                        ReceiverListUiState(emptyList(), ReceiverListLoadState.Loading)
                    }

                    is ReceiverListState.Loading -> {
                        ReceiverListUiState(
                            receivers = state.previousReceivers?.toItems() ?: shownReceivers,
                            loadState = ReceiverListLoadState.Loading,
                        )
                    }

                    is ReceiverListState.Success -> {
                        ReceiverListUiState(state.receivers.toItems(), ReceiverListLoadState.Ready)
                    }

                    is ReceiverListState.Failure -> {
                        failureUiState(state.previousReceivers)
                    }
                }
            repositoryCarriedReceivers = state.carriedReceivers() != null
            shownReceivers = uiState.receivers
            return uiState
        }

        private fun failureUiState(previousReceivers: List<Receiver>?): ReceiverListUiState {
            val receivers =
                when {
                    previousReceivers != null -> previousReceivers.toItems()

                    // 같은 구독이 싣던 목록을 null 로 바꿨다. 저장소가 401 로 버린 목록이라 이 행도 버린다.
                    repositoryCarriedReceivers -> emptyList()

                    // 저장소 구독이 아직 목록을 모른다. 5초를 넘겨 새로 구독한 첫 조회 실패면 보이던 행이 남는다.
                    else -> shownReceivers
                }
            return if (receivers.isEmpty()) {
                ReceiverListUiState(emptyList(), ReceiverListLoadState.Failure)
            } else {
                ReceiverListUiState(receivers, ReceiverListLoadState.RefreshFailure)
            }
        }
    }

private fun List<Receiver>.toItems(): List<ReceiverListItem> = map { ReceiverListItem(it.receiverId, it.name, it.relation) }

private fun ReceiverListState.carriedReceivers(): List<Receiver>? =
    when (this) {
        is ReceiverListState.Loading -> previousReceivers
        is ReceiverListState.Success -> receivers
        is ReceiverListState.Failure -> previousReceivers
        ReceiverListState.SignedOut -> null
    }
