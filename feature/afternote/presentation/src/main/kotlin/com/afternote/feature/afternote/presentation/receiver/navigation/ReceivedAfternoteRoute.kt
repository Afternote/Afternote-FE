package com.afternote.feature.afternote.presentation.receiver.navigation

import kotlinx.serialization.Serializable

/**
 * 수신 애프터노트 화면의 라우트.
 *
 * 발신자 라우트([com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute])
 * 와 한 sealed 계층에 두지 않는다 — 그쪽은 지문 관문을 시작점으로 삼는
 * [com.afternote.core.ui.Route.Afternote] 그래프에 묶여 있고, 수신자는 로그인 사용자가 아니라
 * 그 관문을 지나지 않는다. 그래서 이 라우트들은 그래프 중첩 없이 루트 NavHost 에 직접 등록한다
 * ([com.afternote.feature.afternote.presentation.receiver.navigation.receivedAfternoteNavGraph]).
 *
 * 수신자 흐름의 나머지(받은 기록함·발신자 상세·열람 신청)는 여전히
 * [com.afternote.core.ui.Route.Receiver] 그래프가 갖는다 — 세 피처 공통 진입 인프라이기 때문이다.
 */
sealed interface ReceivedAfternoteRoute {
    /** 수신한 애프터노트 페이지드 목록. */
    @Serializable
    data object ListRoute : ReceivedAfternoteRoute

    /** 수신 애프터노트 상세. */
    @Serializable
    data class DetailRoute(
        val afternoteId: Long,
    ) : ReceivedAfternoteRoute

    /**
     * 수신 추억 플레이리스트 — 추억 상세
     * ([com.afternote.feature.afternote.presentation.receiver.detail.MemorialReceivedDetailScreen]) 의
     * "추억 플레이리스트" 카드 클릭 진입.
     */
    @Serializable
    data class MemorialPlaylistRoute(
        val afternoteId: Long,
    ) : ReceivedAfternoteRoute
}
