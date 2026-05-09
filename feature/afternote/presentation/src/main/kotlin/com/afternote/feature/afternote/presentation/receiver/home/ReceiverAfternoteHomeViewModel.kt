package com.afternote.feature.afternote.presentation.receiver.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 수신자 애프터노트 목록(Home) 화면 ViewModel.
 *
 * 페이지네이션·새로고침은 Paging 3가 담당하며, ViewModel은 카테고리 필터만 보유한다.
 * 카테고리 변경 시 [ReceiverRepository.getPagedReceivedAfternotes]가 새 PagingSource를
 * 발급해 자동으로 재로딩된다.
 */
@HiltViewModel
class ReceiverAfternoteHomeViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val _selectedTab = MutableStateFlow(AfternoteCategory.ALL)
        val selectedTab: StateFlow<AfternoteCategory> = _selectedTab.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedAfternotes: Flow<PagingData<ListItemUiModel>> =
            _selectedTab
                .flatMapLatest { tab ->
                    receiverRepository
                        .getPagedReceivedAfternotes(tab.navKey)
                        .map { pagingData -> pagingData.map { it.toUiModel() } }
                }.cachedIn(viewModelScope)

        fun selectTab(tab: AfternoteCategory) {
            if (_selectedTab.value == tab) return
            _selectedTab.value = tab
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
