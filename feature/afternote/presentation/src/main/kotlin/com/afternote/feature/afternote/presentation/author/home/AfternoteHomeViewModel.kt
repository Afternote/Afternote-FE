package com.afternote.feature.afternote.presentation.author.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 10
private const val SUBSCRIBE_TIMEOUT_MS = 5_000L

/**
 * 애프터노트 목록 화면 ViewModel.
 *
 * UI 액션은 개별 public 메서드로 노출한다 (MVI 식 단일 onEvent 래퍼 미사용).
 */
@HiltViewModel
class AfternoteHomeViewModel
    @Inject
    constructor(
        private val afternoteRepository: AfternoteRepository,
    ) : ViewModel() {
        // MutableStateFlow는 인자로 받은 객체 타입의 MutableStateFlow 객체를 만든다
        // 인자 객체에 대해 equals()를 통해 직전 객체와 비교하여 다를 때마다 리컴포지션하라는 신호를 보낸다 (distinctUntilChanged)
        // 이런 StateFlow의 특성 때문에 상태가 변했을 때만 _uiState가 업데이트됨
        // .value가 set 될 때마다 collect 안으로 _uiState.value를 발행하는 Hot Flow
        private val _uiState = MutableStateFlow(AfternoteHomeUiState())

        // 관찰만 하고 수정할 수 없는 StateFlow 타입으로 변환
        val uiState: StateFlow<AfternoteHomeUiState> = _uiState.asStateFlow()

        val bodyUiState: StateFlow<AfternoteBodyUiState> =
            // _uiState나 uiState나 내부적으로는 동일한 데이터 스트림이지만,
            // ViewModel 내부 조작임을 명시적으로 나타내기 위해 보통 _uiState 원본을 참조한다
            _uiState
                // uiState가 관찰되는 순간 수집되기 전에 전처리처럼 수행될 연산의 설계도
                // 관찰을 시작하는 순간 stateIn을 통해 list의 map처럼 작동 시작
                .map { homeState ->
                    // _uiState.value가 발행될 때마다 실행
                    // 연산 완료했으면 새로운 _uiState.value가 발행될 때까지 suspend
                    val listState = homeState.listState
                    AfternoteBodyUiState(
                        isLoading = listState.isLoading,
                        isRefreshing = listState.isRefreshing,
                        visibleItems = listState.visibleItems.map { it.toUiModel() },
                        selectedCategory = homeState.categoryState.selectedCategory,
                        hasNext = listState.hasNext,
                        isLoadingMore = listState.isLoadingMore,
                        paginationError = listState.paginationError,
                    )
                }.stateIn(
                    // map이 Flow로 매핑했던 것들을 StateFlow로 변환
                    scope = viewModelScope, // map 연산을 수행할 코루틴의 스코프
                    started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), // 관찰자가 있을 때만 map 연산을 수행하게 함
                    initialValue = AfternoteBodyUiState(visibleItems = emptyList()), // bodyUiState의 초기값
                )

        private var fetchJob: Job? = null
        private var loadMoreJob: Job? = null

        init {
            refreshList()
            observeRevisionChanges()
        }

        private fun observeRevisionChanges() {
            viewModelScope.launch {
                afternoteRepository.authorAfternoteListRevision.drop(1).collect {
                    refreshList(
                        category =
                            _uiState.value.categoryState.selectedCategory
                                .toCategoryParam(),
                    )
                }
            }
        }

        // region UI Actions

        fun selectTab(tab: AfternoteCategory) {
            if (_uiState.value.categoryState.selectedCategory == tab) return

            _uiState.update {
                it.copy(categoryState = it.categoryState.copy(selectedCategory = tab))
            }
            refreshList(category = tab.toCategoryParam())
        }

        fun refresh() {
            refreshList(
                category =
                    _uiState.value.categoryState.selectedCategory
                        .toCategoryParam(),
                preserveVisibleItems = true,
            )
        }

        /**
         * 다음 페이지를 로드하여 목록에 이어붙입니다.
         */
        fun loadMore() {
            if (loadMoreJob?.isActive == true) return

            val state = _uiState.value
            val list = state.listState
            if (!list.hasNext || list.isLoadingMore) return

            loadMoreJob =
                viewModelScope.launch {
                    updateListState { it.copy(isLoadingMore = true) }
                    val nextPageNumber = list.currentPageNumber + 1
                    afternoteRepository
                        .getListPage(
                            category = state.categoryState.selectedCategory.toCategoryParam(),
                            pageNumber = nextPageNumber,
                            size = PAGE_SIZE,
                        ).onSuccess { listPage ->
                            updateListState { listState ->
                                listState.copy(
                                    visibleItems = listState.visibleItems + listPage.listItems,
                                    currentPageNumber = nextPageNumber,
                                    isLoadingMore = false,
                                    hasNext = listPage.hasNext,
                                )
                            }
                        }.onFailure { e ->
                            updateListState { listState ->
                                listState.copy(
                                    isLoadingMore = false,
                                    paginationError = e.message ?: "애프터노트 추가 목록을 불러오지 못했습니다.",
                                )
                            }
                        }
                }
        }

        fun consumePaginationError() {
            updateListState { it.copy(paginationError = null) }
        }

        // endregion

        // region Data Loading

        /**
         * API에서 애프터노트 목록 첫 페이지를 로드합니다.
         * 탭 변경 시에도 이 함수를 호출하여 0페이지부터 새로 요청합니다.
         *
         * @param preserveVisibleItems PullToRefresh 경로처럼 기존 리스트를 유지한 채
         *  갱신할 때 true. 이 경우 전체 로딩 UI 대신 [AfternoteListState.isRefreshing] 스피너만 노출한다.
         */
        private fun refreshList(
            category: String? = null,
            preserveVisibleItems: Boolean = false,
        ) {
            fetchJob?.cancel()
            loadMoreJob?.cancel() // race condition 방지: 진행 중이던 페이징 응답이 refresh 이후 상태를 덮어쓰는 걸 차단
            fetchJob =
                viewModelScope.launch {
                    updateListState { listState ->
                        listState.copy(
                            isLoading = !preserveVisibleItems,
                            isRefreshing = preserveVisibleItems,
                            loadError = null,
                            visibleItems = if (preserveVisibleItems) listState.visibleItems else emptyList(),
                        )
                    }
                    afternoteRepository
                        .getListPage(
                            category = category,
                            pageNumber = 0,
                            size = PAGE_SIZE,
                        ).onSuccess { listPage ->
                            updateListState { listState ->
                                listState.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    loadError = null,
                                    visibleItems = listPage.listItems,
                                    currentPageNumber = 0,
                                    hasNext = listPage.hasNext,
                                    isLoadingMore = false,
                                )
                            }
                        }.onFailure { e ->
                            updateListState { listState ->
                                if (preserveVisibleItems) {
                                    listState.copy(
                                        isRefreshing = false,
                                        paginationError =
                                            e.message ?: "목록을 새로고침하지 못했습니다.",
                                    )
                                } else {
                                    listState.copy(
                                        isLoading = false,
                                        isRefreshing = false,
                                        visibleItems = emptyList(),
                                        currentPageNumber = 0,
                                        loadError =
                                            e.message ?: "애프터노트 목록을 불러오지 못했습니다.",
                                        hasNext = false,
                                        isLoadingMore = false,
                                    )
                                }
                            }
                        }
                }
        }

        // endregion

        // region Utility

        /** AfternoteCategory → API category 파라미터 변환. ALL이면 null. */
        private fun AfternoteCategory.toCategoryParam(): String? = navKey

        private fun updateListState(reducer: (AfternoteListState) -> AfternoteListState) {
            _uiState.update { uiState ->
                uiState.copy(listState = reducer(uiState.listState))
            }
        }

        // endregion
    }

private fun ListItem.toUiModel(): ListItemUiModel =
    ListItemUiModel(
        id = id,
        serviceName = serviceName,
        date = date,
        iconResId = getIconResForServiceName(serviceName),
    )
