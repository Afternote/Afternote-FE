package com.afternote.feature.afternote.presentation.author.detail.afternotedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Detail screen UI state (dropdown, delete dialog).
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
 * 상세 화면 생명주기 동안 한 번만 [AfternoteDetailState] 를 유지한다.
 * (불필요한 `remember` 키로 인해 바텀 탭 등 외부 값 변화 시 드롭다운·다이얼로그 상태가 초기화되는 것을 방지)
 */
@Composable
fun rememberAfternoteDetailState(): AfternoteDetailState =
    remember {
        AfternoteDetailState()
    }
