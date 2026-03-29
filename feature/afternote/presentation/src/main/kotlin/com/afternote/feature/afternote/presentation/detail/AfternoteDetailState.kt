package com.afternote.feature.afternote.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.afternote.core.ui.component.bottombar.BottomNavTab

/*
 * Detail screen UI state (dropdown, delete dialog, bottom nav).
 * Used by [SocialNetworkDetailScreen] and
 * [GalleryDetailScreen].
 *
 * @param defaultBottomNavItem 기본 선택된 하단 네비게이션 아이템
 */
@Stable
class AfternoteDetailState(
    defaultBottomNavItem: BottomNavTab = BottomNavTab.NOTE,
) {
    var selectedBottomNavItem by mutableStateOf(defaultBottomNavItem)
        private set

    var showDropdownMenu by mutableStateOf(false)
        private set

    var showDeleteDialog by mutableStateOf(false)
        private set

    /**
     * 하단 네비게이션 아이템 선택
     */
    fun onBottomNavItemSelected(navTab: BottomNavTab) {
        selectedBottomNavItem = navTab
    }

    /**
     * 드롭다운 메뉴 표시/숨김 토글
     */
    fun toggleDropdownMenu() {
        showDropdownMenu = !showDropdownMenu
    }

    /**
     * 드롭다운 메뉴 숨김
     */
    fun hideDropdownMenu() {
        showDropdownMenu = false
    }

    /**
     * 삭제 다이얼로그 표시
     */
    fun showDeleteDialog() {
        showDeleteDialog = true
    }

    /**
     * 삭제 다이얼로그 숨김
     */
    fun hideDeleteDialog() {
        showDeleteDialog = false
    }
}

/**
 * Creates [AfternoteDetailState] for detail screens (social network, gallery).
 *
 * @param defaultBottomNavTab 기본 선택된 하단 네비게이션 아이템
 */
@Stable
@Composable
fun rememberAfternoteDetailState(defaultBottomNavTab: BottomNavTab = BottomNavTab.NOTE): AfternoteDetailState =
    remember(defaultBottomNavTab) {
        AfternoteDetailState(defaultBottomNavItem = defaultBottomNavTab)
    }
