package com.afternote.feature.afternote.presentation.author.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForService
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
 * 페이지네이션·새로고침은 Paging 3가 담당하며, ViewModel은 카테고리 필터만 보유한다.
 * CUD 후 목록 갱신은 Repository 내부에서 자동 처리되므로 수동 refresh는 불필요하다.
 */
@HiltViewModel
class AfternoteHomeViewModel
    @Inject
    constructor(
        private val afternoteRepository: AfternoteRepository,
    ) : ViewModel() {
        /** 선택된 종류 필터. `null` 은 전체다. */
        private val _selectedType = MutableStateFlow<AfternoteType?>(null)
        val selectedType: StateFlow<AfternoteType?> = _selectedType.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedAfternotes: Flow<PagingData<ListItemUiModel>> =
            _selectedType
                .flatMapLatest { type ->
                    afternoteRepository
                        .getPagedAfternotes(type)
                        .map { pagingData -> pagingData.map { it.toUiModel() } }
                }.cachedIn(viewModelScope)

        fun selectTab(tab: AfternoteType?) {
            if (_selectedType.value == tab) return
            _selectedType.value = tab
        }
    }

private fun ListItem.toUiModel(): ListItemUiModel =
    ListItemUiModel(
        id = id,
        serviceName = serviceName,
        date = date,
        iconResId = getIconResForService(serviceName),
        type = type,
    )
