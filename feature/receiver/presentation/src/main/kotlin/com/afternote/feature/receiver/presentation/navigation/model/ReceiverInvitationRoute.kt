package com.afternote.feature.receiver.presentation.navigation.model

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 카카오톡 초대 랜딩 [com.afternote.core.ui.Route.ReceiverInvitation] 그래프 내부 라우트 (#944).
 *
 * 토큰은 라우트에 싣지 않는다 — 비밀값이고, 기기 저장소가 정본이라 ViewModel 이 거기서 읽는다.
 */
sealed interface ReceiverInvitationRoute : NavKey {
    /** 랜딩(4996:40023) — 초대자 확인과 수락·보류. */
    @Serializable
    data object LandingRoute : ReceiverInvitationRoute

    /** 수락 완료(4996:39921). 랜딩을 대체하므로 back 으로 되돌아가지 않는다. */
    @Serializable
    data class CompleteRoute(
        val inviterName: String,
    ) : ReceiverInvitationRoute
}
