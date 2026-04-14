package com.afternote.feature.afternote.presentation.receiver.home
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

/**
 * 수신자 애프터노트 목록 화면에서 발생하는 사용자 이벤트.
 */
sealed interface ReceiverAfternoteHomeEvent {
    data class SelectTab(
        val tab: AfternoteCategory,
    ) : ReceiverAfternoteHomeEvent

    data class SelectBottomNav(
        val navItem: BottomNavTab,
    ) : ReceiverAfternoteHomeEvent

    data class ClickItem(
        val itemId: String,
    ) : ReceiverAfternoteHomeEvent
}
