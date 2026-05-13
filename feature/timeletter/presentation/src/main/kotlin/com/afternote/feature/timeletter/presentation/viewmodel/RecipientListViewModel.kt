package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.data.cache.ReceiverCacheStore
import com.afternote.core.model.setting.ReceiverListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipientListViewModel
    @Inject
    constructor(
        private val receiverCacheStore: ReceiverCacheStore,
    ) : ViewModel() {
        val recipients: StateFlow<List<ReceiverListItem>> =
            receiverCacheStore.receiverList.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        init {
            viewModelScope.launch { receiverCacheStore.ensureLoaded() }
        }
    }
