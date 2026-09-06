package com.afternote.feature.afternote.presentation.receiver.afternotelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.map
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.component.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForService
import com.afternote.feature.receiver.domain.model.AfterNoteListItem
import com.afternote.feature.receiver.domain.repository.ReceiverRepository
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
        /** 선택된 종류 필터. `null` 은 전체다. */
        private val _selectedTab = MutableStateFlow<AfternoteType?>(null)
        val selectedTab: StateFlow<AfternoteType?> = _selectedTab.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val pagedAfternotes: Flow<PagingData<ListItemUiModel>> =
            _selectedTab
                .flatMapLatest { tab ->
                    receiverRepository
                        .getPagedReceivedAfternotes()
                        .map { pagingData ->
                            pagingData
                                .map { it.toUiModel() }
                                .filter { tab == null || it.type == tab }
                        }
                }.cachedIn(viewModelScope)

        fun selectTab(tab: AfternoteType?) {
            if (_selectedTab.value == tab) return
            _selectedTab.value = tab
        }
    }

/** 카드 주 텍스트는 서비스명이, 미등록 서비스 아이콘과 필터 탭은 종류가 결정한다. */
internal fun AfterNoteListItem.toUiModel(): ListItemUiModel =
    ListItemUiModel(
        id = id,
        serviceName = serviceName,
        date = lastUpdatedAt.orEmpty(),
        iconResId = getIconResForService(serviceName, type),
        type = type,
    )
