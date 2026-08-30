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

        fun refresh() {
            refreshRequests.value++
        }
    }
