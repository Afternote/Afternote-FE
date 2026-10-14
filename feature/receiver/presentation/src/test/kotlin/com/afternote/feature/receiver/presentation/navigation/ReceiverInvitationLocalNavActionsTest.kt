package com.afternote.feature.receiver.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverInvitationRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/** 초대 랜딩 로컬 스택 — 완료는 랜딩을 대체한다 (#944). */
class ReceiverInvitationLocalNavActionsTest {
    private val backStack = NavBackStack<NavKey>(ReceiverInvitationRoute.LandingRoute)
    private val actions = ReceiverInvitationLocalNavActions(backStack)

    @Test
    fun `수락 완료는 랜딩을 남기지 않고 완료 화면 하나만 둔다`() {
        actions.replaceLandingWithComplete("김혜성")

        assertEquals(listOf(ReceiverInvitationRoute.CompleteRoute("김혜성")), backStack.toList())
    }
}
