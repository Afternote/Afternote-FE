package com.afternote.feature.afternote.presentation.receiver.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import com.afternote.feature.afternote.presentation.shared.util.getAfternoteDisplayRes
import com.afternote.feature.afternote.presentation.shared.util.getServiceNameForTypeKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 애프터노트 목록(Home) 화면 ViewModel.
 *
 * 수신자 측은 authCode 단발 호출로 전체 목록을 받아 클라이언트에서 필터만 적용한다.
 * 작성자 측 화면과 동일한 LazyPagingItems API를 공유하기 위해 [PagingData.from]으로
 * 정적 리스트를 단일 페이지 PagingData로 감싼다.
 */
@HiltViewModel
class ReceiverAfternoteHomeViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val allItems = MutableStateFlow<List<ListItemUiModel>>(emptyList())
        private val selectedTab = MutableStateFlow(AfternoteCategory.ALL)
        private val isListLoading = MutableStateFlow(true)

        val uiState: StateFlow<ReceiverAfternoteHomeUiState> =
            combine(allItems, selectedTab, isListLoading) { items, tab, loading ->
                val filtered =
                    if (tab == AfternoteCategory.ALL) {
                        items
                    } else {
                        items.filter {
                            AfternoteServiceCatalog.serviceTypeFor(it.serviceName).name == tab.name
                        }
                    }
                ReceiverAfternoteHomeUiState(
                    selectedTab = tab,
                    visibleItems = filtered,
                    isLoading = loading,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS),
                initialValue = ReceiverAfternoteHomeUiState(),
            )

        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedAfternotes: Flow<PagingData<ListItemUiModel>> =
            uiState
                .map { state -> PagingData.from(state.visibleItems) }
                .cachedIn(viewModelScope)

        init {
            loadAfternotes()
        }

        fun onEvent(event: ReceiverAfternoteHomeEvent) {
            when (event) {
                is ReceiverAfternoteHomeEvent.SelectTab -> {
                    selectedTab.value = event.tab
                }

                is ReceiverAfternoteHomeEvent.SelectBottomNav -> {}

                is ReceiverAfternoteHomeEvent.ClickItem -> {}
            }
        }

        private fun loadAfternotes() {
            viewModelScope.launch {
                isListLoading.value = true
                val authCode = receiverRepository.currentAuthCode()
                if (authCode == null) {
                    isListLoading.value = false
                    return@launch
                }
                receiverRepository
                    .getAfterNotesByAuthCode(authCode)
                    .onSuccess { result ->
                        allItems.value = result.items.map { it.toUiModel() }
                    }
                isListLoading.value = false
            }
        }

        private companion object {
            const val SUBSCRIBE_TIMEOUT_MS = 5_000L
        }
    }

private fun AfterNoteListItemDto.toUiModel(): ListItemUiModel {
    val typeKey = sourceType.orEmpty()
    val displayRes = getAfternoteDisplayRes(typeKey)
    val serviceName = getServiceNameForTypeKey(typeKey)
    return ListItemUiModel(
        id = id.toString(),
        serviceName = serviceName,
        date = lastUpdatedAt.orEmpty(),
        iconResId = displayRes.drawableResId,
        type = AfternoteServiceCatalog.serviceTypeFor(serviceName),
    )
}
