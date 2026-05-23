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
     */
    fun onNavigateToSenderRegistration()

    /**
     * 받은 기록함 카드 클릭 → 발신자 상세(11·12) 진입.
     */
    fun onNavigateToSenderDetail(senderId: String)

    /**
     * 발신자 상세의 "열람 신청하기" 진입 — nested 열람 신청 흐름 그래프
     * ([com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute.DeliveryVerificationFlowRoute])
     * 진입점. 본인 확인 캐시 분기는 흐름 내부(IntroRoute 의 LaunchedEffect) 에서 자동 처리되므로 호출자는
     * senderId 만 전달.
     */
    fun onRequestVerificationFlow(senderId: String)

    /**
     * 본인 확인 안내(2) 의 "인증 시작하기" → 이메일 인증 화면(3·4) 진입.
     */
    fun onNavigateIdentityIntroToEmail()

    /**
     * 본인 확인 안내(2) 진입 시점에 캐시가 이미 있어 안내를 생략하고 마스터 키(5) 로 직진하는 경로.
     * Intro 화면은 pop (사용자가 뒤로가기로 안내를 다시 보지 않도록).
     */
    fun onNavigateIdentityIntroToMasterKey()

    /**
     * 본인 확인 이메일 인증 성공 → 마스터 키 입력(5) 진입. 이전 본인 확인 화면 2 장은 pop.
     */
    fun onNavigateIdentityEmailToMasterKey()

    /**
     * 마스터 키 검증 성공 → 증빙 서류 업로드(6·7·8) 진입. 마스터 키 화면은 pop 한다.
     */
    fun onNavigateMasterKeyToDocumentUpload()

    /**
     * 서류 업로드 + `submitDeliveryVerification` 성공 → 완료(9) 진입. 서류 화면은 pop 한다.
     */
    fun onNavigateDocumentUploadToComplete()

    /**
     * 완료(9)의 "받은 기록함으로 돌아가기" → 받은 기록함까지 pop.
     */
    fun onNavigateCompleteToReceivedRecords()

    /**
     * 발신자 상세(12)의 "기록 열람하기" → 수신자 홈 진입. 발신자 컨텍스트(authCode) 복원은
     * `SenderDetailViewModel.openReceiverHome` 이 담당하므로 본 액션은 순수 네비게이션만 수행한다.
     */
    fun onNavigateToReceiverHome()
}
