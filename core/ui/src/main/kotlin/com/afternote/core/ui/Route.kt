package com.afternote.core.ui

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Home : Route

    /** 홈 탭 MEMORIES 영역에서 진입하는 기억 공간(격자·카드 탐색 UI). */
    @Serializable
    data object MemorySpace : Route

    @Serializable
    data object MindRecord : Route

    /**
     * 수신자(추모자)가 발신자에게서 전달받은 마음의 기록을 보는 진입점.
     *
     * 발신자 측 [MindRecord] 와 별개의 top-level 라우트. 동일한 `feature/mindrecord` 모듈 안에
     * 발신자/수신자 화면을 모두 두되, 진입점만 분리해 IA·권한·API(`receiver-auth` prefix)를
     * 명확히 구분한다.
     */
    @Serializable
    data object ReceiverMindRecord : Route

    @Serializable
    data object TimeLetter : Route

    @Serializable
    data object Afternote : Route

    /**
     * 수신자(추모자) 흐름의 그래프 루트.
     *
     * 작성자(`Route.Afternote`)와 별개로 받은 사람이 진입하는 별도 사용자 여정이며,
     * 데이터 레이어도 별도 인증(`X-Auth-Code`)을 사용한다. 진입점(수신자 전용 온보딩)은
     * 디자인 미정으로 후속 PR에서 연결한다 — 본 그래프는 내부 라우팅만 정의한다.
     */
    @Serializable
    data object Receiver : Route

    /** 임시 로그아웃 진입용 설정 화면. 정식 설정 IA 확정 전까지 단일 화면. */
    @Serializable
    data object Setting : Route
}
