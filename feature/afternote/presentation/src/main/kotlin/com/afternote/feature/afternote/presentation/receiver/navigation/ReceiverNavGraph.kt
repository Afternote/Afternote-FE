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
import com.afternote.feature.afternote.presentation.receiver.navigation.model.ReceiverRoute
import com.afternote.feature.afternote.presentation.receiver.playlist.MemorialPlaylistScreen
import com.afternote.feature.afternote.presentation.receiver.playlist.ReceiverMemorialPlaylistViewModel
import com.afternote.feature.afternote.presentation.receiver.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.afternote.presentation.receiver.senderdetail.SenderDetailScreen

/**
 * [Route.Receiver] 내부 화면과 열람 신청 nested graph를 구성한다.
 *
 * @param homeContent 앱 모듈이 제공하는 수신자 홈.
 * @param actions 그래프 내부 이동 명령.
 * @param deliveryFlowParentEntry 열람 신청 graph의 parent entry를 지연 조회하는 함수. Parent entry는
 *   해당 graph에 진입한 뒤에만 존재한다.
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
                onSenderClick = { sender -> actions.navigateToSenderDetail(sender.recordBoxId) },
            )
        }

        receiverComposable<ReceiverRoute.SenderDetailRoute> { backStackEntry ->
            SenderDetailScreen(
                onBackClick = actions::popBack,
                onRequestVerification = actions::navigateToDeliveryVerificationFlow,
                onOpenReceiverHome = actions::navigateToReceiverHome,
            )
        }

        navigation<ReceiverRoute.DeliveryVerificationFlowRoute>(
            startDestination = ReceiverRoute.IdentityVerificationIntroRoute,
        ) {
            receiverComposable<ReceiverRoute.IdentityVerificationIntroRoute> { backStackEntry ->
                val flowVm = backStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry)
                val isVerified by flowVm.isIdentityVerified.collectAsStateWithLifecycle()
                // 캐시 hit에서는 Intro를 그리지 않아 첫 프레임 깜박임을 막는다.
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

            receiverComposable<ReceiverRoute.MasterKeyRoute> {
                MasterKeyScreen(
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
                navigateToDetail = actions::navigateToReceivedAfternoteDetail,
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

/** 열람 신청 graph의 parent entry에 ViewModel을 스코프해 자식 화면들이 같은 인스턴스를 공유한다. */
@Composable
private fun NavBackStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry: () -> NavBackStackEntry): DeliveryVerificationFlowViewModel {
    val parentEntry = remember(this) { deliveryFlowParentEntry() }
    return hiltViewModel(parentEntry)
}
