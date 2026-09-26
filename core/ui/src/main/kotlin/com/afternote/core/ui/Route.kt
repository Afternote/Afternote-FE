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
     * 수신 애프터노트(목록·상세·추억 플레이리스트) 로컬 스택의 host destination.
     *
     * [Afternote] 그래프에 넣지 않는다 — 그쪽 시작점은 발신자용 지문 관문이고 수신자는 그
     * 관문을 지나지 않는다. Nav3 이관 전에는 화면 셋이 루트에 흩어져 있었는데, 로컬 스택을
     * 가지려면 그 스택을 담을 자리가 하나 필요해 route 를 세웠다 (#1698).
     */
    @Serializable
    data object ReceivedAfternote : Route

    /**
     * 수신자(추모자) 흐름의 그래프 루트.
     *
     * 작성자(`Route.Afternote`)와 별개로 받은 사람이 진입하는 별도 사용자 여정이며,
     * 데이터 레이어도 별도 인증(`X-Auth-Code`)을 사용한다. 온보딩 Welcome 의
     * "전달 받은 기록 확인하기" 콜백이 본 라우트로 진입하며, 받은 기록함이 시작 화면이다.
     */
    @Serializable
    data object Receiver : Route

    /**
     * 설정 로컬 스택의 host destination (#1695).
     *
     * 설정 안의 화면은 전부 이 한 칸 아래의 로컬 Nav3 스택에 있다. 홈의 «수신인 지정 미완료» 칩은
     * 설정 홈을 거치지 않고 등록 화면으로 곧장 들어오는데, 로컬 스택의 시작점은 host 가 정하므로
     * 그 진입을 [startWithRecipientRegistration] 인자로 나른다.
     */
    @Serializable
    data class Setting(
        val startWithRecipientRegistration: Boolean = false,
    ) : Route
}
