package com.afternote.feature.afternote.presentation.author.home.model

import com.afternote.feature.afternote.domain.model.author.ListItem
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 카테고리(필터) 탭 상태. 변경 시 목록은 0페이지부터 다시 요청합니다.
 */
data class BodyCategoryState(
    val selectedCategory: AfternoteCategory = AfternoteCategory.ALL,
)

/**
 * 목록·페이지네이션·로딩·에러. 스크롤/추가 로딩과 생명주기가 같습니다.
 *
 * [isLoading]은 첫 진입/탭 변경 시 전체 로딩 UI를 띄우기 위한 플래그이고,
 * [isRefreshing]은 PullToRefresh 스피너 전용이다. 두 상태는 동시에 true가 되지 않는다.
 */
data class AfternoteListState(
    val visibleItems: List<ListItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadError: String? = null,
    val paginationError: String? = null,
    val currentPageNumber: Int = 0,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)

/**
 * 애프터노트 목록(홈) 화면 UI 상태.
 *
 * 페이지네이션에 필요한 상태는 [AfternoteListState]에 두고, 카테고리 필터와
 * 분리하여 단일 진실 공급원(Single Source of Truth)을 유지합니다.
 * 탭(카테고리) 변경 시 서버에 0페이지부터 새로 요청합니다.
 *
 * BottomNav 선택 상태는 앱 레벨(AppState)에서 관리하므로 피처 ViewModel은 보유하지 않는다.
 *
 * 관심사별로 [categoryState] / [listState]로 나누어, 한 구역만 갱신할 때
 * 나머지 불변 스냅샷을 재사용할 수 있게 합니다.
 */
data class AfternoteHomeUiState(
    val categoryState: BodyCategoryState = BodyCategoryState(),
    val listState: AfternoteListState = AfternoteListState(isLoading = true),
)
