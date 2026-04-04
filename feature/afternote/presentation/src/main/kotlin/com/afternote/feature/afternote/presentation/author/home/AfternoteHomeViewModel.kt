package com.afternote.feature.afternote.presentation.author.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.usecase.author.GetListPageUseCase
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeEvent
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeUiState
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
class AfternoteHomeViewModel
    @Inject
    constructor(
        private val getListPageUseCase: GetListPageUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AfternoteHomeUiState())
        val uiState: StateFlow<AfternoteHomeUiState> = _uiState.asStateFlow()

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
         * 탭 변경 시에도 이 함수를 호출하여 0페이지부터 새로 요청합니다.
         */
        fun loadAfternotes(category: String? = null) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        loadError = null,
                        items = emptyList(),
                    )
                }
                val input =
                    GetListPageInput(
                        category = category,
                        page = 0,
                        size = PAGE_SIZE,
                    )
                getListPageUseCase(input)
                    .onSuccess { paged ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = null,
                                items = paged.items,
                                currentPage = 0,
                                hasNext = paged.hasNext,
                                isLoadingMore = false,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
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
                getListPageUseCase(input)
                    .onSuccess { paged ->
                        _uiState.update {
                            it.copy(
                                items = it.items + paged.items,
                                currentPage = nextPage,
                                isLoadingMore = false,
                                hasNext = paged.hasNext,
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
            }
        }

        fun onEvent(event: AfternoteHomeEvent) {
            when (event) {
                is AfternoteHomeEvent.SelectTab -> {
                    val currentState = _uiState.value
                    if (currentState.selectedTab == event.tab) return

                    _uiState.update { it.copy(selectedTab = event.tab) }
                    loadAfternotes(category = event.tab.toCategoryParam())
                }

                is AfternoteHomeEvent.SelectBottomNav -> {
                    _uiState.update { it.copy(selectedBottomNavItem = event.navItem) }
                }
            }
        }

        /** AfternoteCategory → API category 파라미터 변환. ALL이면 null. */
        private fun AfternoteCategory.toCategoryParam(): String? =
            when (this) {
                AfternoteCategory.ALL -> null
                else -> label
            }
    }
