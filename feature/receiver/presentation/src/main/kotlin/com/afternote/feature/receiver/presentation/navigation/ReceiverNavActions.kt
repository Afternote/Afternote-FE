package com.afternote.feature.receiver.presentation.navigation

/**
 * 수신자 로컬 스택 안에서만 의미가 있는 이동 명령.
 *
 * 구현은 스택을 가진 [ReceiverNavHost] 안에 있다 — 앱 셸은 더 이상 이 명령을 만들지 않는다.
 * 열람 신청 흐름 5단계 사이의 이동은 흐름 전용 스택을 가진 `DeliveryVerificationFlowNavActions`
 * 가 갖는다 (#1698).
 *
 * 다른 top-level Route(MindRecord/TimeLetter/Setting 등)로의 이동은 수신자 홈 측에서
 * 별도 `ReceiverHomeActions`로 받는다.
 *
 * 작명 컨벤션 (#239): `navigateTo<Where>` / `popBack` / `popTo<Where>` /
 * `replace<X>With<Y>` / `proceedTo<Next>`.
 */
interface ReceiverNavActions {
    fun popBack()

    /** 받은 기록함의 FAB → 발신자 등록 화면(이슈 #215, 디자인 15·16) 진입. */
    fun navigateToSenderRegistration()

    /** 받은 기록함 카드 클릭 → 발신자 상세(11·12) 진입. */
    fun navigateToSenderDetail(senderId: String)

    /**
     * 발신자 상세의 "열람 신청하기" → nested 열람 신청 흐름 그래프
     * ([com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute.DeliveryVerificationFlowRoute])
     * 진입. 본인 확인 캐시 분기는 흐름 내부(IntroRoute 의 LaunchedEffect) 에서 자동 처리되므로 호출자는
     * senderId 만 전달.
     */
    fun navigateToDeliveryVerificationFlow(senderId: String)

    /** 열람 신청 완료(9)의 "받은 기록함으로 돌아가기" → 받은 기록함 위를 모두 걷어낸다. */
    fun popToReceivedRecords()

    /**
     * 발신자 상세(12)의 "기록 열람하기" → 수신자 홈 진입. 발신자 컨텍스트(masterKey) 복원은
     * `SenderDetailViewModel.openReceiverHome` 이 담당하므로 본 액션은 순수 네비게이션만 수행한다.
     */
    fun navigateToReceiverHome()
}
