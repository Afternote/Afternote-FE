package com.afternote.feature.afternote.presentation.author.home.model
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 애프터노트 목록 화면 UI 이벤트.
 * ViewModel에서 상태를 변경하는 이벤트만 정의합니다.
 * 네비게이션(아이템 클릭, 추가)은 Route에서 직접 처리합니다.
 */
sealed interface AfternoteHomeEvent {
    data class SelectTab(
        val tab: AfternoteCategory,
    ) : AfternoteHomeEvent

    data class SelectBottomNav(
        val navItem: BottomNavTab,
    ) : AfternoteHomeEvent

    data object Refresh : AfternoteHomeEvent

    data object LoadMore : AfternoteHomeEvent

    data object PaginationErrorConsumed : AfternoteHomeEvent
}
