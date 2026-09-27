package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.setting.ReceiverListItem
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
        private val receiverRepository: UserReceiverRepository,
    ) : ViewModel() {
        val receivers: StateFlow<List<ReceiverListItem>> =
            receiverRepository.receiverListFlow
                .map { receivers -> receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) } }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )
    }
