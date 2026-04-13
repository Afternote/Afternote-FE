package com.afternote.feature.afternote.presentation.receiver.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import com.afternote.feature.afternote.presentation.shared.util.getAfternoteDisplayRes
import com.afternote.feature.afternote.presentation.shared.util.getServiceNameForTypeKey
import dagger.hilt.android.lifecycle.HiltViewModel
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

        private val uiState: StateFlow<ReceiverAfternoteHomeUiState> =
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
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ReceiverAfternoteHomeUiState(),
            )

        val bodyUiState: StateFlow<AfternoteBodyUiState> =
            uiState
                .map { state ->
                    AfternoteBodyUiState(
                        isLoading = state.isLoading,
                        visibleItems = state.visibleItems,
                        selectedCategory = state.selectedTab,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000L),
                    initialValue = AfternoteBodyUiState(visibleItems = emptyList()),
                )

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
    }

private fun AfterNoteListItemDto.toUiModel(): ListItemUiModel {
    val typeKey = sourceType.orEmpty()
    val displayRes = getAfternoteDisplayRes(typeKey)
    return ListItemUiModel(
        id = id.toString(),
        serviceName = getServiceNameForTypeKey(typeKey),
        date = lastUpdatedAt.orEmpty(),
        iconResId = displayRes.drawableResId,
    )
}
