package com.afternote.feature.afternote.presentation.author.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.usecase.author.GetListUseCase
import com.afternote.feature.afternote.presentation.author.list.model.AfternoteListEvent
import com.afternote.feature.afternote.presentation.author.list.model.AfternoteListUiState
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 10
private const val SUBSCRIBE_TIMEOUT_MS = 5_000L

/**
 * 애프터노트 목록 화면 ViewModel.
 */
@HiltViewModel
class AfternoteListViewModel
    @Inject
    constructor(
        private val getAfternotesUseCase: GetListUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AfternoteListUiState())
        val uiState: StateFlow<AfternoteListUiState> = _uiState.asStateFlow()

        /** uiState에서 자동 파생되는 Screen용 UI 상태. */
        val bodyUiState: StateFlow<AfternoteBodyUiState> =
            _uiState
                .map { state ->
                    AfternoteBodyUiState(
                        items =
                            state.items.map { item ->
                                ListItemUiModel(
                                    id = item.id,
                                    serviceName = item.serviceName,
                                    date = item.date,
                                    iconResId = getIconResForServiceName(item.serviceName),
                                )
                            },
                        selectedTab = state.selectedTab,
                        hasNext = state.hasNext,
                        isLoadingMore = state.isLoadingMore,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS),
                    initialValue = AfternoteBodyUiState(items = emptyList()),
                )

        init {
            loadAfternotes()
        }

        /**
         * API에서 애프터노트 목록 첫 페이지를 로드합니다.
         */
        fun loadAfternotes(category: String? = null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, loadError = null) }
                val input =
                    GetListPageInput(
                        category = category,
                        page = 0,
                        size = PAGE_SIZE,
                    )
                getAfternotesUseCase(input)
                    .onSuccess { paged ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = null,
                                allItems = paged.items,
                                currentPage = 0,
                                hasNext = paged.hasNext,
                                isLoadingMore = false,
                            )
                        }
                        applyFilter()
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                allItems = emptyList(),
                                items = emptyList(),
                                currentPage = 0,
                                loadError = e.message ?: "애프터노트 목록을 불러오지 못했습니다.",
                                hasNext = false,
                                isLoadingMore = false,
                            )
                        }
                    }
            }
        }

        /**
         * 다음 페이지를 로드하여 목록에 이어붙입니다.
         */
        fun loadNextPage() {
            val state = _uiState.value
            if (!state.hasNext || state.isLoadingMore) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingMore = true) }
                val nextPage = state.currentPage + 1
                val input =
                    GetListPageInput(
                        category = state.selectedTab.toCategoryParam(),
                        page = nextPage,
                        size = PAGE_SIZE,
                    )
                getAfternotesUseCase(input)
                    .onSuccess { paged ->
                        _uiState.update {
                            it.copy(
                                allItems = it.allItems + paged.items,
                                currentPage = nextPage,
                                isLoadingMore = false,
                                hasNext = paged.hasNext,
                            )
                        }
                        applyFilter()
                    }.onFailure {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
            }
        }

        fun onEvent(event: AfternoteListEvent) {
            when (event) {
                is AfternoteListEvent.SelectTab -> {
                    _uiState.update { it.copy(selectedTab = event.tab) }
                    applyFilter()
                }

                is AfternoteListEvent.SelectBottomNav -> {
                    _uiState.update { it.copy(selectedBottomNavItem = event.navItem) }
                }
            }
        }

        /** 선택된 탭에 따라 allItems를 필터링하여 items에 반영합니다. */
        private fun applyFilter() {
            _uiState.update { state ->
                val filtered =
                    when (state.selectedTab) {
                        AfternoteCategory.ALL -> {
                            state.allItems
                        }

                        AfternoteCategory.SOCIAL_NETWORK -> {
                            state.allItems.filter { it.type == AfternoteServiceType.SOCIAL_NETWORK }
                        }

                        AfternoteCategory.GALLERY_AND_FILES -> {
                            state.allItems.filter { it.type == AfternoteServiceType.GALLERY_AND_FILES }
                        }

                        AfternoteCategory.MEMORIAL -> {
                            state.allItems.filter { it.type == AfternoteServiceType.MEMORIAL }
                        }
                    }
                state.copy(items = filtered)
            }
        }

        /** AfternoteCategory → API category 파라미터 변환. ALL이면 null. */
        private fun AfternoteCategory.toCategoryParam(): String? =
            when (this) {
                AfternoteCategory.ALL -> null
                else -> label
            }
    }
