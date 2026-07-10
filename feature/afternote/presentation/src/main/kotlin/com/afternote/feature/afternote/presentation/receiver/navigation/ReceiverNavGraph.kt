package com.afternote.feature.afternote.presentation.receiver.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DeliveryVerificationCompleteScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DeliveryVerificationFlowViewModel
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.DocumentUploadScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.IdentityVerificationIntroScreen
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.MasterKeyScreen
import com.afternote.feature.afternote.presentation.receiver.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.afternote.presentation.receiver.home.ReceiverAfternoteHomeEntry
import com.afternote.feature.afternote.presentation.receiver.home.ReceiverAfternoteHomeEntryActions
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import com.afternote.feature.afternote.presentation.receiver.playlist.MemorialPlaylistScreen
import com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistViewModel
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
 * 열람 신청 흐름(2·3·4·5·6·7·8·9) 은 [ReceiverRoute.DeliveryVerificationFlowRoute] 를 진입점으로 한
 * nested graph 로 묶여, `senderId` 와 본인 확인 캐시 등 흐름 상태를
 * [DeliveryVerificationFlowViewModel] 한 곳에 보유한다 (#220).
 *
 * @param homeContent 앱 모듈이 주입하는 수신자 홈 Composable 슬롯.
 * @param actions 그래프 내부 이동(받은 기록함/목록/상세/발신자 등록·상세/열람 신청) 명령.
 * @param deliveryFlowParentEntry [ReceiverRoute.DeliveryVerificationFlowRoute] 의 NavBackStackEntry 를
 *   *호출 시점에* 가져오는 람다. `NavGraphBuilder` 확장 함수는 `NavController` 를 직접 받지 못하므로
 *   호출처(`AppNavigation`)가 `navController.getBackStackEntry<DeliveryVerificationFlowRoute>()` 를
 *   closure 로 capture 해 주입한다. parent entry 는 NavGraph 빌드 시점에는 존재하지 않고 사용자가
 *   nested graph 에 navigate 한 *후* 에 만들어지므로 *값* 이 아닌 *람다* 로 받는 것이 필수 (lazy 평가).
 *   자식 composable scope 에서 호출되면 그 시점의 parent entry 가 반환되어
 *   [DeliveryVerificationFlowViewModel] 인스턴스를 공유하는 데 쓰인다.
 */
fun NavGraphBuilder.receiverNavGraph(
    homeContent: @Composable () -> Unit,
    actions: ReceiverNavActions,
    deliveryFlowParentEntry: () -> NavBackStackEntry,
) {
    navigation<Route.Receiver>(startDestination = ReceiverRoute.ReceivedRecordsRoute) {
        receiverComposable<ReceiverRoute.ReceivedRecordsRoute> {
            ReceivedRecordsScreen(
                onBackClick = actions::popBack,
                onAddSenderClick = actions::navigateToSenderRegistration,
                onSenderClick = { sender -> actions.navigateToSenderDetail(sender.id) },
            )
        }

        receiverComposable<ReceiverRoute.SenderRegistrationRoute> {
            SenderRegistrationScreen(
                onBackClick = actions::popBack,
                // 등록 완료 시 받은 기록함으로 pop. 카드는 자동으로 반영된다 (SenderRegistry StateFlow).
                onRegistered = actions::popBack,
            )
        }

        receiverComposable<ReceiverRoute.SenderDetailRoute> { backStackEntry ->
            val senderId = backStackEntry.toRoute<ReceiverRoute.SenderDetailRoute>().senderId
            SenderDetailScreen(
                onBackClick = actions::popBack,
                onRequestVerification = { actions.navigateToDeliveryVerificationFlow(senderId) },
                onOpenReceiverHome = actions::navigateToReceiverHome,
            )
        }

        // 열람 신청 흐름 nested graph — senderId·본인 확인 캐시는 flow VM 에서 공유.
        navigation<ReceiverRoute.DeliveryVerificationFlowRoute>(
            startDestination = ReceiverRoute.IdentityVerificationIntroRoute,
        ) {
            receiverComposable<ReceiverRoute.IdentityVerificationIntroRoute> { backStackEntry ->
                val flowVm = backStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry)
                val isVerified by flowVm.isIdentityVerified.collectAsStateWithLifecycle()
                // 본인 확인 캐시가 있으면 안내 화면 paint 자체를 skip 하고 즉시 마스터 키로 점프.
                // (이전엔 IntroScreen 을 분기 밖에 그려서 첫 프레임 ~16ms 동안 안내가 비치는
                //  깜박임이 있었음. `if (!isVerified)` 로 분기해 paint 가드.)
                //
                // Intro 는 jump 직후 백스택에서 사라지고, 이후 단계(MasterKey→DocumentUpload→Complete) 의
                // popUpTo 들도 모두 inclusive 라 흐름 중 Intro 가 다시 등장하지 않는다.
                if (isVerified) {
                    LaunchedEffect(Unit) {
                        actions.proceedToMasterKey()
                    }
                } else {
                    IdentityVerificationIntroScreen(
                        onBackClick = actions::popBack,
                        onStartClick = actions::navigateToIdentityVerificationEmail,
                    )
                }
            }

            receiverComposable<ReceiverRoute.IdentityVerificationEmailRoute> {
                IdentityVerificationEmailScreen(
                    onBackClick = actions::popBack,
                    onVerified = actions::proceedToMasterKey,
                )
            }

            receiverComposable<ReceiverRoute.MasterKeyRoute> { backStackEntry ->
                val flowVm = backStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry)
                MasterKeyScreen(
                    senderId = flowVm.senderId,
                    onBackClick = actions::popBack,
                    onVerified = actions::proceedToDocumentUpload,
                )
            }

            receiverComposable<ReceiverRoute.DocumentUploadRoute> {
                DocumentUploadScreen(
                    onBackClick = actions::popBack,
                    onSubmitted = actions::proceedToDeliveryVerificationComplete,
                )
            }

            receiverComposable<ReceiverRoute.DeliveryVerificationCompleteRoute> {
                DeliveryVerificationCompleteScreen(
                    onBackToRecords = actions::popToReceivedRecords,
                )
            }
        }

        receiverComposable<ReceiverRoute.HomeRoute> {
            homeContent()
        }

        receiverComposable<ReceiverRoute.AfternoteListRoute> {
            ReceiverAfternoteHomeEntry(
                actions =
                    ReceiverAfternoteHomeEntryActions(
                        navigateToDetail = actions::navigateToReceivedAfternoteDetail,
                    ),
            )
        }

        receiverComposable<ReceiverRoute.AfternoteDetailRoute> {
            ReceivedAfternoteDetailRoute(
                onBack = actions::popBack,
                onNavigateToPlaylist = actions::navigateToMemorialPlaylist,
            )
        }

        receiverComposable<ReceiverRoute.MemorialPlaylistRoute> {
            val playlistViewModel: ReceiverMemorialPlaylistViewModel = hiltViewModel()
            val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
            MemorialPlaylistScreen(
                senderName = playlistUiState.senderName,
                songs = playlistUiState.songs,
                onBackClick = actions::popBack,
            )
        }
    }
}

/**
 * 자식 라우트 composable scope 에서 parent [ReceiverRoute.DeliveryVerificationFlowRoute] backStackEntry 를
 * 받아 동일한 [DeliveryVerificationFlowViewModel] 인스턴스를 공유한다.
 *
 * **동작 순서**
 * 1. 자식 backStackEntry 를 receiver 로 받아 [deliveryFlowParentEntry] 람다를 호출 → parent entry 획득.
 * 2. `hiltViewModel(parentEntry)` 가 parent entry 의 `ViewModelStore` 안에서 동일 VM 인스턴스를 lookup.
 *    parent entry 는 nested graph 가 백스택에 살아있는 동안 같은 인스턴스 → 자식 5 화면 모두 같은 VM 공유.
 * 3. nested graph 를 빠져나가면 parent entry 가 pop 되며 VM 도 `onCleared` 로 자동 정리 (flow-scoped).
 *
 * `remember(this)` wrap — 자식 composable 재구성마다 `deliveryFlowParentEntry()` 재호출하지 않고 캐싱된 parent
 * entry 재사용. Compose Navigation 공식 권장 패턴
 * (developer.android.com/develop/ui/compose/libraries 의 "ViewModel scoped to navigation graph" 예제).
 */
@Composable
private fun NavBackStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry: () -> NavBackStackEntry): DeliveryVerificationFlowViewModel {
    val parentEntry = remember(this) { deliveryFlowParentEntry() }
    return hiltViewModel(parentEntry)
}
