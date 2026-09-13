package com.afternote.feature.receiver.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 수신자 로컬 백스택 경계 회귀 기준 — Nav2 시절 `AuthBoundaryBackStackTest` 가 보던 몫 (#1601 · #1698).
 */
class ReceiverLocalNavActionsTest {
    private var exits = 0
    private val backStack = NavBackStack<NavKey>(ReceiverRoute.ReceivedRecordsRoute)
    private val actions =
        ReceiverLocalNavActions(
            backStack = backStack,
            boundary = FeatureStackBoundary { exits += 1 },
        )

    private fun stack(): List<String> = backStack.map { it::class.simpleName!! }

    @Test
    fun `발신자 상세와 열람 신청 흐름은 받은 기록함 위에 차례로 쌓인다`() {
        actions.navigateToSenderDetail(SENDER_ID)
        actions.navigateToDeliveryVerificationFlow(SENDER_ID)

        assertEquals(
            listOf("ReceivedRecordsRoute", "SenderDetailRoute", "DeliveryVerificationFlowRoute"),
            stack(),
        )
    }

    @Test
    fun `흐름 진입 키가 senderId 를 나른다`() {
        actions.navigateToDeliveryVerificationFlow(SENDER_ID)

        val flowKey = backStack.last() as ReceiverRoute.DeliveryVerificationFlowRoute
        assertEquals(SENDER_ID, flowKey.senderId)
    }

    @Test
    fun `받은 기록함으로 돌아가기는 그 위를 모두 걷어낸다`() {
        actions.navigateToSenderDetail(SENDER_ID)
        actions.navigateToDeliveryVerificationFlow(SENDER_ID)

        actions.popToReceivedRecords()

        assertEquals(listOf("ReceivedRecordsRoute"), stack())
    }

    @Test
    fun `스택 바닥에서의 뒤로가기는 스택을 비우지 않고 셸에 넘긴다`() {
        actions.popBack()

        assertEquals(listOf("ReceivedRecordsRoute"), stack())
        assertEquals(1, exits)
    }

    private companion object {
        const val SENDER_ID = "sender-1601"
    }
}
