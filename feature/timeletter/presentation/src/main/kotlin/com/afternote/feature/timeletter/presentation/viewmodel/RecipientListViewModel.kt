package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.setting.ReceiverListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecipientListViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val refreshRequests = MutableStateFlow(0)

        val recipients: StateFlow<List<ReceiverListItem>> =
            refreshRequests
                .flatMapLatest { userRepository.receiverListFlow }
                .map { receivers -> receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) } }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 [recipients] 의 최초
         * 구독과 같은 진입이므로 갱신하지 않는다 — VM 필드인 이유는 ReceiverHomeViewModel 의
         * refreshOnReturn 과 동일, 프로세스 사망 후 복원에서도 최초 구독과 수명이 일치한다.
         */
        private var isFirstResume = true

        fun refresh() {
            refreshRequests.value++
        }

        /**
         * 다른 화면에서 복귀했을 때의 자동 갱신 (#701). 첫 진입은 건너뛴다 — `flatMapLatest` 가
         * 겹치는 구독을 알아서 취소하므로 별도의 진행 중 로드 가드는 필요 없다.
         */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            refresh()
        }
    }
