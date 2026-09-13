package com.afternote.feature.receiver.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.popOrExit
import com.afternote.core.ui.navigation.popUpTo
import com.afternote.core.ui.navigation.replaceAllWith
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute

/**
 * 수신자 화면 콜백을 로컬 백스택 조작으로 잇는다.
 *
 * 컴포저블이 아니라 평범한 클래스다 — 백스택 «모양» 을 컴포지션 없이 재려는 것이다(#1601).
 */
internal class ReceiverLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
) : ReceiverNavActions {
    override fun popBack(): Unit = backStack.popOrExit(boundary)

    override fun navigateToSenderRegistration() {
        backStack.add(ReceiverRoute.SenderRegistrationRoute)
    }

    override fun navigateToSenderDetail(senderId: String) {
        backStack.add(ReceiverRoute.SenderDetailRoute(senderId = senderId))
    }

    override fun navigateToDeliveryVerificationFlow(senderId: String) {
        backStack.add(ReceiverRoute.DeliveryVerificationFlowRoute(senderId = senderId))
    }

    override fun navigateToReceiverHome() {
        backStack.add(ReceiverRoute.HomeRoute)
    }

    /** 받은 기록함을 남기고 그 위(발신자 상세·열람 신청 흐름)를 모두 걷어낸다. */
    override fun popToReceivedRecords(): Unit = backStack.popUpTo(ReceiverRoute.ReceivedRecordsRoute)
}

/**
 * 열람 신청 흐름 안에서만 의미가 있는 이동. 바깥 스택을 건드리는 둘은 콜백으로 위임한다.
 *
 * 단계마다 «지나온 화면을 남기지 않는» 것이 이 흐름의 규칙이다 — 인증번호·마스터 키는 서버가
 * 이미 소비했으므로 뒤로가기로 되돌아가면 안 된다.
 */
internal class DeliveryVerificationFlowLocalNavActions(
    private val stepStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val onExitToReceivedRecords: () -> Unit,
) : DeliveryVerificationFlowNavActions {
    override fun popBack(): Unit = stepStack.popOrExit(boundary)

    override fun navigateToIdentityVerificationEmail() {
        stepStack.add(ReceiverRoute.IdentityVerificationEmailRoute)
    }

    override fun proceedToMasterKey(): Unit = stepStack.replaceAllWith(ReceiverRoute.MasterKeyRoute)

    override fun proceedToDocumentUpload(): Unit = stepStack.replaceAllWith(ReceiverRoute.DocumentUploadRoute)

    override fun proceedToDeliveryVerificationComplete(): Unit = stepStack.replaceAllWith(ReceiverRoute.DeliveryVerificationCompleteRoute)

    override fun popToReceivedRecords(): Unit = onExitToReceivedRecords()
}
