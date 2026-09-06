package com.afternote.feature.afternote.presentation.receiver.navigation

/**
 * 수신 애프터노트 화면의 이동 명령.
 *
 * 라우트 상수를 화면이 직접 들지 않도록 앱 모듈 구현으로 캡슐화한다 — 수신자 흐름의
 * `ReceiverNavActions`(:feature:receiver:presentation) 와 같은 규약이다.
 */
interface ReceivedAfternoteNavActions {
    fun popBack()

    /**
     * 수신 상세 하단 "애프터노트 확인하기" → 목록 진입.
     *
     * 목록에서 상세로 들어와 다시 목록을 부르는 왕복이라 스택을 쌓지 않는다 (#777).
     */
    fun navigateToList()

    /** 목록 항목 클릭 → 수신 애프터노트 상세 진입. */
    fun navigateToDetail(afternoteId: Long)

    /** 추억 상세의 "추억 플레이리스트" 카드 → 추억 플레이리스트 화면 진입 (#274). */
    fun navigateToMemorialPlaylist(afternoteId: Long)
}
