package com.afternote.feature.receiver.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationScreen
import com.afternote.feature.receiver.presentation.senderdetail.SenderDetailScreen

/**
 * 수신자 피처가 소유하는 로컬 Navigation 3 스택 — 받은 기록함부터 열람 신청 흐름까지.
 *
 * 열람 신청 5단계는 이 스택에 직접 쌓지 않고 [DeliveryVerificationFlowHost] 가 여는 흐름 전용
 * 스택으로 내린다. 흐름 ViewModel 의 수명(5화면 공유 → 흐름 이탈 시 정리)을 그 entry 범위로
 * 보존하기 위해서다.
 *
 * @param homeContent 앱 셸이 주는 수신자 홈 — 다른 피처의 수신 화면을 조합하므로 셸이 만든다.
 * @param boundary 스택 바닥에서의 back 경계.
 */
@Composable
public fun ReceiverNavHost(
    homeContent: @Composable () -> Unit,
    boundary: FeatureStackBoundary,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(ReceiverRoute.ReceivedRecordsRoute)
    val actions = remember(backStack, boundary) { ReceiverLocalNavActions(backStack, boundary) }

    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<ReceiverRoute.ReceivedRecordsRoute> {
                    ReceiverTheme {
                        ReceivedRecordsScreen(
                            onBackClick = actions::popBack,
                            onAddSenderClick = actions::navigateToSenderRegistration,
                            onSenderClick = { sender -> actions.navigateToSenderDetail(sender.id) },
                        )
                    }
                }

                entry<ReceiverRoute.SenderRegistrationRoute> {
                    ReceiverTheme {
                        SenderRegistrationScreen(
                            onBackClick = actions::popBack,
                            onRegistered = actions::popBack,
                        )
                    }
                }

                entry<ReceiverRoute.SenderDetailRoute> { key ->
                    ReceiverTheme {
                        SenderDetailScreen(
                            onBackClick = actions::popBack,
                            onRequestVerification = { actions.navigateToDeliveryVerificationFlow(key.senderId) },
                            onOpenReceiverHome = actions::navigateToReceiverHome,
                        )
                    }
                }

                entry<ReceiverRoute.DeliveryVerificationFlowRoute> { key ->
                    ReceiverTheme {
                        DeliveryVerificationFlowHost(
                            key = key,
                            onExitFlow = actions::popBack,
                            onExitToReceivedRecords = actions::popToReceivedRecords,
                        )
                    }
                }

                entry<ReceiverRoute.HomeRoute> {
                    ReceiverTheme {
                        homeContent()
                    }
                }
            },
    )
}
