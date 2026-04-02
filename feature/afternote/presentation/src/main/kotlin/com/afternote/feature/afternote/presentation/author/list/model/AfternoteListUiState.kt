package com.afternote.feature.afternote.presentation.author.list.model
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.domain.model.Item
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 애프터노트 목록 화면 UI 상태.
 *
 * 페이지네이션·필터링에 필요한 모든 상태를 하나의 data class에 캡슐화하여
 * 단일 진실 공급원(Single Source of Truth)을 유지합니다.
 */
data class AfternoteListUiState(
    val selectedTab: AfternoteCategory = AfternoteCategory.ALL,
    val selectedBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
    /** 전체 아이템 (필터링 전) */
    val allItems: List<Item> = emptyList(),
    /** 선택된 탭에 따라 필터링된 아이템 */
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val currentPage: Int = 0,
    val hasNext: Boolean = false,
    val isLoadingMore: Boolean = false,
)
