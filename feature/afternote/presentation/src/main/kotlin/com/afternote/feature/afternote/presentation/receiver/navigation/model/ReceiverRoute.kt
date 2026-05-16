package com.afternote.feature.afternote.presentation.receiver.navigation.model

import kotlinx.serialization.Serializable

/**
 * 수신자 흐름 [com.afternote.core.ui.Route.Receiver] 그래프 내부 라우트.
 *
 * 진입점(수신자 전용 온보딩)은 디자인 미정으로 미연결 상태이며, 본 그래프는
 * 그래프 내부 라우팅(Home → AfternoteList → AfternoteDetail)만 정의한다.
 */
sealed interface ReceiverRoute {
    /** 수신자 대시보드 — 발신자 한 마디 + 마음의 기록·타임레터·애프터노트 섹션 카드. */
    @Serializable
    data object HomeRoute : ReceiverRoute

    /** 수신한 애프터노트 페이지드 목록. */
    @Serializable
    data object AfternoteListRoute : ReceiverRoute

    /**
     * 수신 애프터노트 상세.
     *
     * 프로퍼티 이름은 [com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailViewModel] 의
     * `SavedStateHandle` 키(`afternoteId`)와 일치해야 한다(typed-safe routes 규약).
     */
    @Serializable
    data class AfternoteDetailRoute(
        val afternoteId: String,
    ) : ReceiverRoute
}
