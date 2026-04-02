package com.afternote.feature.afternote.presentation.receiver.model
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 수신자 애프터노트 목록 화면에서 발생하는 사용자 이벤트.
 */
sealed interface ReceiverAfternoteListEvent {
    data class SelectTab(
        val tab: AfternoteCategory,
    ) : ReceiverAfternoteListEvent

    data class SelectBottomNav(
        val navItem: BottomNavTab,
    ) : ReceiverAfternoteListEvent

    data class ClickItem(
        val itemId: String,
    ) : ReceiverAfternoteListEvent
}
