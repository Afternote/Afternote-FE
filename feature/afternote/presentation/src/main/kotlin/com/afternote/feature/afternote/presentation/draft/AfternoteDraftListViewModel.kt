package com.afternote.feature.afternote.presentation.draft

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import androidx.paging.map
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.shared.component.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 임시저장 목록.
 *
 * 서버는 발행분과 임시저장을 한 요청에 섞어 주지 않아(`draftOnly` 미전송 = 발행분만) 목록이 따로 선다.
 * 종류 필터를 두지 않는 이유는 이 화면의 용도가 «이어쓸 것 고르기» 하나뿐이라서다 — 홈처럼 훑는 화면이 아니다.
 */
@HiltViewModel
internal class AfternoteDraftListViewModel
    @Inject
    constructor(
        afternoteRepository: AfternoteRepository,
    ) : MviViewModel<Nothing, AfternoteDraftListUiState, AfternoteDraftListReducerEvent>(AfternoteDraftListUiState()) {
        init {
            val drafts =
                afternoteRepository
                    .getPagedDrafts(type = null)
                    .map { pagingData -> pagingData.map { it.toUiModel() } }
                    .cachedIn(viewModelScope)
            viewModelScope.launch {
                drafts.collect { dispatch(AfternoteDraftListReducerEvent.PageGenerationChanged(it)) }
            }
        }

        // Retry/refresh belong to Paging; navigation is handled by the entry callbacks.
        // This read-only ViewModel has no user intents.
        override fun onIntent(intent: Nothing): Unit = intent

        override fun reduce(
            state: AfternoteDraftListUiState,
            event: AfternoteDraftListReducerEvent,
        ): AfternoteDraftListUiState =
            when (event) {
                is AfternoteDraftListReducerEvent.PageGenerationChanged -> state.copy(items = event.items)
            }
    }
