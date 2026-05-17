package com.afternote.feature.afternote.presentation.receiver.navigation

/**
 * NavHost 루트에서 수신자 서브그래프로 넘기는 그래프 내부 네비게이션 명령.
 *
 * 다른 top-level Route(MindRecord/TimeLetter/Setting 등)로의 이동은 수신자 홈 측에서
 * 별도 `ReceiverHomeActions`로 받는다 — 본 인터페이스는 [com.afternote.core.ui.Route.Receiver]
 * 그래프 내부에서만 의미가 있는 이동만 다룬다.
 */
interface ReceiverNavActions {
    fun onPopBackStack()

    fun onNavigateToAfternoteList()

    fun onNavigateToReceivedAfternoteDetail(afternoteId: String)

    /**
     * 받은 기록함의 FAB → 발신자 등록 화면(이슈 #215, 디자인 15·16) 진입.
     * 현재(1단계) placeholder. 후속 단계에서 발신자 등록 화면 추가 시 wire-up.
     */
    fun onNavigateToSenderRegistration()
}
