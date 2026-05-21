package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DeliveryVerificationCompleteScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DocumentUploadScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationIntroScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.MasterKeyScreen
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.home.ReceiverAfternoteHomeEntry
import com.afternote.feature.afternote.presentation.receiver.home.ReceiverAfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import com.afternote.feature.afternote.presentation.receiver.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.afternote.presentation.receiver.recordsbox.SenderRegistrationScreen
import com.afternote.feature.afternote.presentation.receiver.senderdetail.SenderDetailScreen

/**
 * 수신자 흐름 네비게이션 그래프. 앱 모듈의 NavHost에 직접 연결되며 [Route.Receiver]를
 * graph route로 사용한다.
 *
 * Welcome 의 "전달 받은 기록 확인하기" 콜백이 [Route.Receiver] (= [ReceiverRoute.ReceivedRecordsRoute])
 * 로 진입한다. 본인 확인은 받은 기록함이 아닌, 발신자 상세에서 "열람 신청하기" 누른 직후 1회성으로 수행한다.
 *
 * 발신자별 수신자 홈([ReceiverRoute.HomeRoute])은 다중 피처(마음의 기록·타임레터·애프터노트) 섹션을
 * 모은 대시보드라 app 모듈에 위치한다. 본 그래프는 [homeContent] 슬롯으로 받아 라우트에 바인딩한다.
 *
 * @param homeContent 앱 모듈이 주입하는 수신자 홈 Composable 슬롯.
 *   `ReceiverHomeEntry`를 자체 액션과 함께 호출해 전달한다.
 * @param actions 그래프 내부 이동(받은 기록함/목록/상세/발신자 등록·상세/열람 신청) 명령.
 */
fun NavGraphBuilder.receiverNavGraph(
    homeContent: @Composable () -> Unit,
    actions: ReceiverNavActions,
) {
    navigation<Route.Receiver>(startDestination = ReceiverRoute.ReceivedRecordsRoute) {
        receiverComposable<ReceiverRoute.ReceivedRecordsRoute> {
            ReceivedRecordsScreen(
                onBackClick = actions::onPopBackStack,
                onAddSenderClick = actions::onNavigateToSenderRegistration,
                onSenderClick = { sender -> actions.onNavigateToSenderDetail(sender.id) },
            )
        }

        receiverComposable<ReceiverRoute.SenderRegistrationRoute> {
            SenderRegistrationScreen(
                onBackClick = actions::onPopBackStack,
                // 등록 완료 시 받은 기록함으로 pop. 카드는 자동으로 반영된다 (SenderRegistry StateFlow).
                onRegistered = actions::onPopBackStack,
            )
        }

        receiverComposable<ReceiverRoute.SenderDetailRoute> { backStackEntry ->
            val senderId = backStackEntry.toRoute<ReceiverRoute.SenderDetailRoute>().senderId
            SenderDetailScreen(
                onBackClick = actions::onPopBackStack,
                onRequestVerification = { actions.onRequestVerificationFlow(senderId) },
                onOpenReceiverHome = actions::onNavigateToReceiverHome,
            )
        }

        receiverComposable<ReceiverRoute.IdentityVerificationIntroRoute> { backStackEntry ->
            val senderId = backStackEntry.toRoute<ReceiverRoute.IdentityVerificationIntroRoute>().senderId
            IdentityVerificationIntroScreen(
                onBackClick = actions::onPopBackStack,
                onStartClick = { actions.onNavigateIdentityIntroToEmail(senderId) },
            )
        }

        receiverComposable<ReceiverRoute.IdentityVerificationEmailRoute> { backStackEntry ->
            val senderId = backStackEntry.toRoute<ReceiverRoute.IdentityVerificationEmailRoute>().senderId
            IdentityVerificationEmailScreen(
                onBackClick = actions::onPopBackStack,
                onVerified = { actions.onNavigateIdentityEmailToMasterKey(senderId) },
            )
        }

        receiverComposable<ReceiverRoute.MasterKeyRoute> { backStackEntry ->
            val senderId = backStackEntry.toRoute<ReceiverRoute.MasterKeyRoute>().senderId
            MasterKeyScreen(
                onBackClick = actions::onPopBackStack,
                onVerified = { actions.onNavigateMasterKeyToDocumentUpload(senderId) },
            )
        }

        receiverComposable<ReceiverRoute.DocumentUploadRoute> { backStackEntry ->
            val senderId = backStackEntry.toRoute<ReceiverRoute.DocumentUploadRoute>().senderId
            DocumentUploadScreen(
                onBackClick = actions::onPopBackStack,
                onSubmitted = { actions.onNavigateDocumentUploadToComplete(senderId) },
            )
        }

        receiverComposable<ReceiverRoute.DeliveryVerificationCompleteRoute> {
            DeliveryVerificationCompleteScreen(
                onBackToRecords = actions::onNavigateCompleteToReceivedRecords,
            )
        }

        receiverComposable<ReceiverRoute.HomeRoute> {
            homeContent()
        }

        receiverComposable<ReceiverRoute.AfternoteListRoute> {
            ReceiverAfternoteHomeEntry(
                actions =
                    ReceiverAfternoteHomeEntryActions(
                        navigateToDetail = actions::onNavigateToReceivedAfternoteDetail,
                    ),
            )
        }

        receiverComposable<ReceiverRoute.AfternoteDetailRoute> {
            ReceivedAfternoteDetailRoute(onBack = actions::onPopBackStack)
        }
    }
}
