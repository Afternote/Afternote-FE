package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.Receiver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ReceiverListUiState {
    data object Loading : ReceiverListUiState

    data class Success(
        val receivers: List<ReceiverListItem>,
    ) : ReceiverListUiState

    data object Error : ReceiverListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReceiverListViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val retryRevision = MutableStateFlow(0L)

        val uiState: StateFlow<ReceiverListUiState> =
            retryRevision
                .flatMapLatest {
                    userRepository.receiverListFlow
                        .map<List<Receiver>, ReceiverListUiState> { receivers ->
                            ReceiverListUiState.Success(
                                receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) },
                            )
                        }.onStart { emit(ReceiverListUiState.Loading) }
                        .catch { emit(ReceiverListUiState.Error) }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = ReceiverListUiState.Loading,
                )

        fun retry() {
            retryRevision.value += 1
        }
    }
