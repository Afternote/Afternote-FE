package com.afternote.feature.afternote.presentation.receiver.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.port.ReceiverAuthCodeProvider
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfterNotesByAuthCodeUseCase
import com.afternote.feature.afternote.presentation.receiver.model.ReceiverAfternoteHomeEvent
import com.afternote.feature.afternote.presentation.receiver.model.uistate.ReceiverAfternoteHomeUiState
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog
import com.afternote.feature.afternote.presentation.shared.util.getAfternoteDisplayRes
import com.afternote.feature.afternote.presentation.shared.util.getServiceNameForTypeKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 애프터노트 목록(Home) 화면 ViewModel.
 *
 * [GetAfterNotesByAuthCodeUseCase]로 목록을 조회하고, 탭/카테고리 필터링 상태를 관리합니다.
 */
@HiltViewModel
class ReceiverAfternoteHomeViewModel
    @Inject
    constructor(
        private val receiverAuthCodeProvider: ReceiverAuthCodeProvider,
        private val getAfterNotesByAuthCodeUseCase: GetAfterNotesByAuthCodeUseCase,
    ) : ViewModel() {
        private val allItems = MutableStateFlow<List<ListItemUiModel>>(emptyList())
        private val selectedTab = MutableStateFlow(AfternoteCategory.ALL)

        val uiState: StateFlow<ReceiverAfternoteHomeUiState> =
            combine(allItems, selectedTab) { items, tab ->
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
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ReceiverAfternoteHomeUiState(),
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
            val authCode = receiverAuthCodeProvider.currentAuthCode() ?: return
            viewModelScope.launch {
                getAfterNotesByAuthCodeUseCase(authCode)
                    .onSuccess { result ->
                        allItems.value = result.items.map { it.toUiModel() }
                    }
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
