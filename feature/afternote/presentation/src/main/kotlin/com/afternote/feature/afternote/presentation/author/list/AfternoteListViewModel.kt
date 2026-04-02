package com.afternote.feature.afternote.presentation.author.list
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.usecase.GetListUseCase
import com.afternote.feature.afternote.presentation.author.list.model.AfternoteListEvent
import com.afternote.feature.afternote.presentation.author.list.model.AfternoteListUiState
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

        private var allItems: List<Item> = emptyList()
        private var currentPage: Int = 0
        private var hasNextPage: Boolean = false
        private val pageSize: Int = 10

        /** Screen에서 바로 사용할 수 있는 UI 상태. */
        val bodyUiState: StateFlow<AfternoteBodyUiState>
            get() = _bodyUiState.asStateFlow()
        private val _bodyUiState = MutableStateFlow(AfternoteBodyUiState(items = emptyList()))

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
                        size = pageSize,
                    )
                getAfternotesUseCase(input)
                    .onSuccess { paged ->
                        allItems = paged.items
                        currentPage = 0
                        hasNextPage = paged.hasNext
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = null,
                                hasNext = paged.hasNext,
                                isLoadingMore = false,
                            )
                        }
                        updateFilteredItems(_uiState.value.selectedTab)
                    }.onFailure { e ->
                        allItems = emptyList()
                        currentPage = 0
                        hasNextPage = false
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = emptyList(),
                                loadError = e.message ?: "애프터노트 목록을 불러오지 못했습니다.",
                                hasNext = false,
                                isLoadingMore = false,
                            )
                        }
                        updateBodyUiState()
                    }
            }
        }

        /**
         * 다음 페이지를 로드하여 목록에 이어붙입니다.
         */
        fun loadNextPage() {
            if (!hasNextPage || _uiState.value.isLoadingMore) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingMore = true) }
                val category: String? = null
                val nextPage = currentPage + 1
                val input =
                    GetListPageInput(
                        category = category,
                        page = nextPage,
                        size = pageSize,
                    )
                getAfternotesUseCase(input)
                    .onSuccess { paged ->
                        allItems = allItems + paged.items
                        currentPage = nextPage
                        hasNextPage = paged.hasNext
                        _uiState.update {
                            it.copy(isLoadingMore = false, hasNext = paged.hasNext)
                        }
                        updateFilteredItems(_uiState.value.selectedTab)
                    }.onFailure {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
            }
        }

        /**
         * UI 이벤트 처리
         */
        fun onEvent(event: AfternoteListEvent) {
            when (event) {
                is AfternoteListEvent.SelectTab -> {
                    updateTab(event.tab)
                }

                is AfternoteListEvent.SelectBottomNav -> {
                    updateBottomNav(event.navItem)
                }

                is AfternoteListEvent.ClickItem -> {
                    // 네비게이션은 Route에서 처리
                }

                is AfternoteListEvent.ClickAdd -> {
                    // 네비게이션은 Route에서 처리
                }
            }
        }

        private fun updateTab(tab: AfternoteCategory) {
            _uiState.update { it.copy(selectedTab = tab) }
            updateFilteredItems(tab)
        }

        private fun updateFilteredItems(tab: AfternoteCategory) {
            val filtered =
                when (tab) {
                    AfternoteCategory.ALL -> allItems
                    AfternoteCategory.SOCIAL_NETWORK ->
                        allItems.filter { it.type == AfternoteServiceType.SOCIAL_NETWORK }
                    AfternoteCategory.GALLERY_AND_FILES ->
                        allItems.filter { it.type == AfternoteServiceType.GALLERY_AND_FILES }
                    AfternoteCategory.MEMORIAL ->
                        allItems.filter { it.type == AfternoteServiceType.MEMORIAL }
                }

            _uiState.update { it.copy(items = filtered) }
            updateBodyUiState()
        }

        private fun updateBottomNav(navItem: BottomNavTab) {
            _uiState.update { it.copy(selectedBottomNavItem = navItem) }
        }

        /** uiState → bodyUiState 변환 (Item → ListItemUiModel 매핑 포함) */
        private fun updateBodyUiState() {
            val state = _uiState.value
            val displayItems =
                state.items.map { item ->
                    ListItemUiModel(
                        id = item.id,
                        serviceName = item.serviceName,
                        date = item.date,
                        iconResId = getIconResForServiceName(item.serviceName),
                    )
                }
            _bodyUiState.value =
                AfternoteBodyUiState(
                    items = displayItems,
                    selectedTab = state.selectedTab,
                    hasNext = state.hasNext,
                    isLoadingMore = state.isLoadingMore,
                )
        }
    }
