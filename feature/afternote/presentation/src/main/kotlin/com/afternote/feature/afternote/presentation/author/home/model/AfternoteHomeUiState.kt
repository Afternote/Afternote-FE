package com.afternote.feature.afternote.presentation.author.home.model

import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 하단 네비(전역 탭) 선택 상태. [BottomNavTab] 전환과 생명주기가 같습니다.
 */
data class AfternoteNavTabState(
    val selectedBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
)

/**
 * 카테고리(필터) 탭 상태. 변경 시 목록은 0페이지부터 다시 요청합니다.
 */
data class BodyCategoryState(
    val selectedCategory: AfternoteCategory = AfternoteCategory.ALL,
)

/**
 * 목록·페이지네이션·로딩·에러. 스크롤/추가 로딩과 생명주기가 같습니다.
 */
data class AfternoteListState(
    val listItems: List<ListItem> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val currentPageNumber: Int = 0,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)

/**
 * 애프터노트 목록(홈) 화면 UI 상태.
 *
 * 페이지네이션에 필요한 상태는 [AfternoteListState]에 두고, 하단 네비·카테고리 필터와
 * 분리하여 단일 진실 공급원(Single Source of Truth)을 유지합니다.
 * 탭(카테고리) 변경 시 서버에 0페이지부터 새로 요청합니다.
 *
 * 관심사별로 [navState] / [categoryState] / [listState]로 나누어, 한 구역만 갱신할 때
 * 나머지 불변 스냅샷을 재사용할 수 있게 합니다.
 */
data class AfternoteHomeUiState(
    val navState: AfternoteNavTabState = AfternoteNavTabState(),
    val categoryState: BodyCategoryState = BodyCategoryState(),
    val listState: AfternoteListState = AfternoteListState(),
)
