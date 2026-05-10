package com.afternote.feature.afternote.presentation.receiver.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.receiver.AfterNoteListItemDto
import com.afternote.feature.afternote.domain.repository.ReceiverRepository
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
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
 * 서버는 페이지네이션을 지원하지 않아 Repository는 단일 페이지 PagingData를 흘리지만,
 * Paging 3 API(LoadState/refresh/cachedIn) 통일을 위해 그대로 사용한다. 카테고리 필터는
 * 서버 파라미터가 없어 클라이언트 사이드 [PagingData.filter]로 적용한다.
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
                        .getPagedReceivedAfternotes()
                        .map { pagingData ->
                            pagingData
                                .map { it.toUiModel() }
                                .filter { tab == AfternoteCategory.ALL || it.type.name == tab.name }
                        }
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
    val type =
        runCatching { AfternoteServiceType.valueOf(typeKey) }
            .getOrDefault(AfternoteServiceType.SOCIAL_NETWORK)
    return ListItemUiModel(
        id = id.toString(),
        serviceName = serviceName,
        date = lastUpdatedAt.orEmpty(),
        iconResId = displayRes.drawableResId,
        type = type,
    )
}
