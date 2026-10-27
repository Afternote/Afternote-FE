package com.afternote.feature.setting.presentation.notification.anniversary

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.NotificationAnniversary
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신인 생일을 등록·삭제한다 (#2321). 그날 서버가 「오늘은 ○○님 생신이에요」 알림을 보내고,
 * 알림을 누르면 그 수신인 앞으로 편지 작성이 열린다.
 */
@HiltViewModel
internal class AnniversaryViewModel
    @Inject
    constructor(
        private val repository: NotificationPreferenceRepository,
        private val receiverRepository: UserReceiverRepository,
    ) : MviViewModel<AnniversaryIntent, AnniversaryUiState, AnniversaryReducerEvent>(AnniversaryUiState()) {
        init {
            load()
        }

        override fun onIntent(intent: AnniversaryIntent) {
            when (intent) {
                AnniversaryIntent.Retry -> load()
                is AnniversaryIntent.Add -> add(intent.receiverId, intent.month, intent.day)
                is AnniversaryIntent.Delete -> delete(intent.anniversaryId)
                AnniversaryIntent.ConsumeError -> dispatch(AnniversaryReducerEvent.ErrorConsumed)
            }
        }

        override fun reduce(
            state: AnniversaryUiState,
            event: AnniversaryReducerEvent,
        ): AnniversaryUiState =
            when (event) {
                AnniversaryReducerEvent.Loading -> {
                    state.copy(isLoading = true, loadFailed = false)
                }

                is AnniversaryReducerEvent.Loaded -> {
                    state.copy(isLoading = false, items = event.items.sortedForDisplay(), receivers = event.receivers)
                }

                AnniversaryReducerEvent.LoadFailed -> {
                    state.copy(isLoading = false, loadFailed = true)
                }

                AnniversaryReducerEvent.Submitting -> {
                    state.copy(isSubmitting = true, errorMessage = null)
                }

                is AnniversaryReducerEvent.Added -> {
                    state.copy(isSubmitting = false, items = (state.items + event.item).sortedForDisplay())
                }

                is AnniversaryReducerEvent.Deleted -> {
                    state.copy(isSubmitting = false, items = state.items.filterNot { it.id == event.anniversaryId })
                }

                is AnniversaryReducerEvent.Failed -> {
                    state.copy(isSubmitting = false, errorMessage = event.message)
                }

                AnniversaryReducerEvent.ErrorConsumed -> {
                    state.copy(errorMessage = null)
                }
            }

        private fun load() {
            dispatch(AnniversaryReducerEvent.Loading)
            viewModelScope.launch {
                val receivers = async { runCatchingCancellable { receiverRepository.getReceivers() } }
                val anniversaries = async { repository.getAnniversaries() }
                val receiverList = receivers.await().getOrNull()
                val anniversaryList = anniversaries.await().getOrNull()
                if (receiverList == null || anniversaryList == null) {
                    dispatch(AnniversaryReducerEvent.LoadFailed)
                    return@launch
                }
                val options = receiverList.map { AnniversaryReceiver(id = it.receiverId, name = it.name) }
                dispatch(AnniversaryReducerEvent.Loaded(anniversaryList.map { it.toItem(options) }, options))
            }
        }

        private fun add(
            receiverId: Long,
            month: Int,
            day: Int,
        ) {
            if (currentState.isSubmitting) return
            dispatch(AnniversaryReducerEvent.Submitting)
            viewModelScope.launch {
                repository
                    .addAnniversary(receiverId, month, day)
                    .onSuccess { dispatch(AnniversaryReducerEvent.Added(it.toItem(currentState.receivers))) }
                    .onFailure { dispatch(AnniversaryReducerEvent.Failed(UiText.Resource(R.string.setting_anniversary_add_failed))) }
            }
        }

        private fun delete(anniversaryId: Long) {
            if (currentState.isSubmitting) return
            dispatch(AnniversaryReducerEvent.Submitting)
            viewModelScope.launch {
                repository
                    .deleteAnniversary(anniversaryId)
                    .onSuccess { dispatch(AnniversaryReducerEvent.Deleted(anniversaryId)) }
                    .onFailure { dispatch(AnniversaryReducerEvent.Failed(UiText.Resource(R.string.setting_anniversary_delete_failed))) }
            }
        }
    }

private fun NotificationAnniversary.toItem(receivers: List<AnniversaryReceiver>): AnniversaryItem =
    AnniversaryItem(
        id = id,
        receiverName = receivers.firstOrNull { it.id == receiverId }?.name,
        month = month,
        day = day,
    )

private fun List<AnniversaryItem>.sortedForDisplay(): List<AnniversaryItem> = sortedWith(compareBy({ it.month }, { it.day }, { it.id }))
