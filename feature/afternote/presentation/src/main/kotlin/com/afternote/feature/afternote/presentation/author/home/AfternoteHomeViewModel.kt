package com.afternote.feature.afternote.presentation.author.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.usecase.author.GetListPageUseCase
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeEvent
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteHomeUiState
import com.afternote.feature.afternote.presentation.author.home.model.AfternoteListState
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory
import com.afternote.feature.afternote.presentation.shared.body.infinite.AfternoteBodyUiState
import com.afternote.feature.afternote.presentation.shared.body.infinite.content.list.item.ListItemUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
        // MutableStateFlow는 인자로 받은 객체 타입의 MutableStateFlow 객체를 만든다
        private val _uiState = MutableStateFlow(AfternoteHomeUiState())

        // 관찰만 하고 수정할 수 없는 StateFlow 타입으로 변환
        val uiState: StateFlow<AfternoteHomeUiState> = _uiState.asStateFlow()
        val bodyUiState: StateFlow<AfternoteBodyUiState> =
            // 새로운 상태를 파생시킬 때 순수한 원천 데이터를 사용하기 위해 uiState가 아닌 원본을 가져 옴
            // 내부 로직은 내부용 변수 _uiState를 쓰는 것이 안전
            _uiState
                // uiState를 관찰하는 순간에 수행할 연산의 설계도
                // 관찰을 시작하는 순간 list의 map처럼 작동
                // _uiState는 map 설계를 했을 뿐 수행은 bodyUiState가 함
                .map { homeState ->
                    // 관찰하는 시점 기준 최신 homeState를 가져옴
                    // 연산이 수행된 시점에 모든 homeState 요소에 대해 연산을 완료했다면 새로운 요소가 추가될 때까지 대기
                    val listState = homeState.listState
                    AfternoteBodyUiState(
                        listItems = listState.listItems.map { it.toUiModel() },
                        selectedCategory = homeState.categoryState.selectedCategory,
                        hasNext = listState.hasNext,
                        isLoadingMore = listState.isLoadingMore,
                    )
                }.stateIn(
                    // map이 Flow로 매핑했던 것들을 StateFlow로 변환
                    scope = viewModelScope, // map 연산을 수행할 코루틴의 스코프
                    started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), // 관찰자가 있을 때만 map 연산을 수행하게 함
                    initialValue = AfternoteBodyUiState(listItems = emptyList()), // bodyUiState의 초기값
                )

        init {
            refreshList()
        }

        /**
         * API에서 애프터노트 목록 첫 페이지를 로드합니다.
         * 탭 변경 시에도 이 함수를 호출하여 0페이지부터 새로 요청합니다.
         */
        private var fetchJob: Job? = null

        fun refreshList(category: String? = null) {
            fetchJob?.cancel()
            fetchJob =
                viewModelScope.launch {
                    updateListState { listState ->
                        listState.copy(
                            isLoading = true,
                            loadError = null,
                            listItems = emptyList(),
                        )
                    }
                    val input =
                        GetListPageInput(
                            category = category,
                            page = 0, // 항상 첫 페이지 불러 옴
                            size = PAGE_SIZE,
                        )
                    getListPageUseCase(input)
                        .onSuccess { listPage ->
                            updateListState { listState ->
                                listState.copy(
                                    isLoading = false,
                                    loadError = null,
                                    listItems = listPage.listItems,
                                    currentPage = 0,
                                    hasNext = listPage.hasNext,
                                    isLoadingMore = false,
                                )
                            }
                        }.onFailure { e ->
                            updateListState { listState ->
                                listState.copy(
                                    isLoading = false,
                                    listItems = emptyList(),
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
        fun loadMoreListItems() {
            viewModelScope.launch {
                val state = _uiState.value
                val list = state.listState
                if (!list.hasNext || list.isLoadingMore) return@launch
                updateListState { listState ->
                    listState.copy(isLoadingMore = true)
                }
                val nextPage = list.currentPage + 1
                val input =
                    GetListPageInput(
                        category = state.categoryState.selectedCategory.toCategoryParam(),
                        page = nextPage,
                        size = PAGE_SIZE,
                    )
                getListPageUseCase(input)
                    .onSuccess { listPage ->
                        updateListState { listState ->
                            listState.copy(
                                listItems = listState.listItems + listPage.listItems,
                                currentPage = nextPage,
                                isLoadingMore = false,
                                hasNext = listPage.hasNext,
                            )
                        }
                    }.onFailure {
                        updateListState { listState ->
                            listState.copy(isLoadingMore = false)
                        }
                    }
            }
        }

        fun onEvent(event: AfternoteHomeEvent) {
            when (event) {
                is AfternoteHomeEvent.SelectTab -> {
                    val currentState = _uiState.value
                    if (currentState.categoryState.selectedCategory == event.tab) return

                    _uiState.update {
                        it.copy(
                            categoryState = it.categoryState.copy(selectedCategory = event.tab),
                        )
                    }
                    refreshList(category = event.tab.toCategoryParam())
                }

                is AfternoteHomeEvent.SelectBottomNav -> {
                    _uiState.update {
                        it.copy(
                            navState =
                                it.navState.copy(
                                    selectedBottomNavItem = event.navItem,
                                ),
                        )
                    }
                }
            }
        }

        /** AfternoteCategory → API category 파라미터 변환. ALL이면 null. */
        private fun AfternoteCategory.toCategoryParam(): String? =
            when (this) {
                AfternoteCategory.ALL -> null
                else -> label
            }

        private fun updateListState(reducer: (AfternoteListState) -> AfternoteListState) {
            _uiState.update { uiState ->
                uiState.copy(listState = reducer(uiState.listState))
            }
        }
    }

private fun ListItem.toUiModel(): ListItemUiModel =
    ListItemUiModel(
        id = id,
        serviceName = serviceName,
        date = date,
        iconResId = getIconResForServiceName(serviceName),
    )
