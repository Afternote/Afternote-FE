package com.afternote.feature.receiver.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.replaceAllWith
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverInvitationRoute

/** 랜딩 → 완료는 대체다 — 수락이 서버에 반영된 뒤라 back 으로 랜딩에 되돌아가지 않는다. */
internal class ReceiverInvitationLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
) {
    fun replaceLandingWithComplete(inviterName: String): Unit = backStack.replaceAllWith(ReceiverInvitationRoute.CompleteRoute(inviterName))
}
