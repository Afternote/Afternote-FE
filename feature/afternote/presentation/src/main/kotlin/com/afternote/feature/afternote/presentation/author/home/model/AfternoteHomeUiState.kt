package com.afternote.feature.afternote.presentation.author.home.model
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.ListItem
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 애프터노트 목록 화면 UI 상태.
 *
 * 페이지네이션에 필요한 모든 상태를 하나의 data class에 캡슐화하여
 * 단일 진실 공급원(Single Source of Truth)을 유지합니다.
 * 탭(카테고리) 변경 시 서버에 0페이지부터 새로 요청합니다.
 */
data class AfternoteHomeUiState(
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val selectedBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
    val listItems: List<ListItem> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val currentPage: Int = 0,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)
