package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import com.afternote.core.ui.Route
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.home.ReceiverAfternoteHomeEntry
import com.afternote.feature.afternote.presentation.receiver.home.ReceiverAfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute

/**
 * 수신자 흐름 네비게이션 그래프. 앱 모듈의 NavHost에 직접 연결되며 [Route.Receiver]를
 * graph route로 사용한다.
 *
 * **진입점(수신자 전용 온보딩)은 디자인 미정으로 본 그래프 외부에서 연결되지 않는다.**
 * 후속 PR이 온보딩 화면을 추가하며 [Route.Receiver]로 navigate 하도록 와이어링할 예정.
 *
 * 수신자 홈은 다중 피처(마음의 기록·타임레터·애프터노트) 섹션을 모은 대시보드라 app 모듈에
 * 위치한다. 본 그래프는 [homeContent] 슬롯으로 받아 [ReceiverRoute.HomeRoute] 라우트에
 * 바인딩한다.
 *
 * @param homeContent 앱 모듈이 주입하는 수신자 홈 Composable 슬롯.
 *   `ReceiverHomeEntry`를 자체 액션과 함께 호출해 전달한다.
 * @param actions 그래프 내부 이동(목록/상세) 명령. 다른 top-level Route 이동은 홈 슬롯
 *   내부에서 별도 액션으로 처리한다.
 */
fun NavGraphBuilder.receiverNavGraph(
    homeContent: @Composable () -> Unit,
    actions: ReceiverNavActions,
) {
    navigation<Route.Receiver>(startDestination = ReceiverRoute.HomeRoute) {
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
