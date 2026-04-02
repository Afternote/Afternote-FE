package com.afternote.feature.afternote.presentation.author.list.model
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 애프터노트 목록 화면 UI 이벤트
 * ViewModel로 전달되는 사용자 행위를 정의
 */
sealed interface AfternoteListEvent {
    /**
     * 탭 선택 이벤트
     */
    data class SelectTab(
        val tab: AfternoteCategory,
    ) : AfternoteListEvent

    /**
     * 하단 네비게이션 아이템 선택 이벤트
     */
    data class SelectBottomNav(
        val navItem: BottomNavTab,
    ) : AfternoteListEvent

    /**
     * 아이템 클릭 이벤트
     */
    data class ClickItem(
        val itemId: String,
    ) : AfternoteListEvent

    /**
     * 추가 버튼 클릭 이벤트
     */
    data object ClickAdd : AfternoteListEvent
}
