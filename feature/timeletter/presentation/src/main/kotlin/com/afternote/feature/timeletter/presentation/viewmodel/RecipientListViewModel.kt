package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.setting.ReceiverListItem
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
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface RecipientListUiState {
    data object Loading : RecipientListUiState

    data class Success(
        val recipients: List<ReceiverListItem>,
    ) : RecipientListUiState

    data class Error(
        val message: String,
    ) : RecipientListUiState
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RecipientListViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val retryRevision = MutableStateFlow(0L)

        val uiState: StateFlow<RecipientListUiState> =
            retryRevision
                .flatMapLatest {
                    userRepository.receiverListFlow
                        .map { receivers ->
                            RecipientListUiState.Success(
                                receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) },
                            ) as RecipientListUiState
                        }.onStart { emit(RecipientListUiState.Loading) }
                        .catch { emit(RecipientListUiState.Error("수신자 목록을 불러올 수 없습니다.")) }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = RecipientListUiState.Loading,
                )

        fun retry() {
            retryRevision.update { it + 1 }
        }
    }
