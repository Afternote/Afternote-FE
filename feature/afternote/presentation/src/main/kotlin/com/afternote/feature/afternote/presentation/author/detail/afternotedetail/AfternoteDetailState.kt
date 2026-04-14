package com.afternote.feature.afternote.presentation.author.detail.afternotedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.afternote.core.ui.bottombar.BottomNavTab

/**
 * Detail screen UI state (dropdown, delete dialog, bottom nav).
 * Used by [SocialNetworkDetailScreen], [GalleryDetailScreen], and
 * [MemorialGuidelineDetailScreen].
 */
@Stable
class AfternoteDetailState {
    var showDropdownMenu by mutableStateOf(false)
        private set

    var showDeleteDialog by mutableStateOf(false)
        private set

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
 * @param defaultBottomNavItem 기본 선택된 하단 네비게이션 아이템
 */
@Stable
@Composable
fun rememberAfternoteDetailState(defaultBottomNavItem: BottomNavTab = BottomNavTab.NOTE): AfternoteDetailState =
    remember(defaultBottomNavItem) {
        AfternoteDetailState()
    }
