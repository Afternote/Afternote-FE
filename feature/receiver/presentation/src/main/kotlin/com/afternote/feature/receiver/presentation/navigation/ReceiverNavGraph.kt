package com.afternote.feature.receiver.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.afternote.core.ui.Route
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.feature.afternote.presentation.author.navigation.DetailLoadErrorContent
import com.afternote.feature.receiver.presentation.afternotelist.ReceiverAfternoteHomeEntry
import com.afternote.feature.receiver.presentation.deliveryverification.DeliveryVerificationCompleteScreen
import com.afternote.feature.receiver.presentation.deliveryverification.DeliveryVerificationFlowViewModel
import com.afternote.feature.receiver.presentation.deliveryverification.DocumentUploadScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationEmailScreen
import com.afternote.feature.receiver.presentation.deliveryverification.IdentityVerificationIntroScreen
import com.afternote.feature.receiver.presentation.deliveryverification.MasterKeyScreen
import com.afternote.feature.receiver.presentation.detail.ReceivedAfternoteDetailRoute
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverRoute
import com.afternote.feature.receiver.presentation.playlist.MemorialPlaylistScreen
import com.afternote.feature.receiver.presentation.playlist.ReceiverMemorialPlaylistUiState
import com.afternote.feature.receiver.presentation.playlist.ReceiverMemorialPlaylistViewModel
import com.afternote.feature.receiver.presentation.recordsbox.ReceivedRecordsScreen
import com.afternote.feature.receiver.presentation.recordsbox.SenderRegistrationScreen
import com.afternote.feature.receiver.presentation.senderdetail.SenderDetailScreen

/**
 * 받은 기록함부터 수신 콘텐츠 상세까지의 수신자 화면을 연결한다.
 * 열람 신청 단계는 중첩 그래프의 [DeliveryVerificationFlowViewModel]을 공유한다.
 *
 * @param homeContent 앱 모듈에서 제공하는 수신자 홈 화면
 * @param actions 수신자 그래프의 화면 이동 명령
 * @param deliveryFlowParentEntry 열람 신청 중첩 그래프의 back stack entry 제공자
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

        // 열람 신청 단계가 발신자와 본인 확인 상태를 공유하는 중첩 그래프
        navigation<ReceiverRoute.DeliveryVerificationFlowRoute>(
            startDestination = ReceiverRoute.IdentityVerificationIntroRoute,
        ) {
            receiverComposable<ReceiverRoute.IdentityVerificationIntroRoute> { backStackEntry ->
                val flowVm = backStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry)
                val isVerified by flowVm.isIdentityVerified.collectAsStateWithLifecycle()
                // 이미 본인 확인을 마친 사용자는 안내 화면을 건너뛴다.
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
                navigateToDetail = actions::navigateToReceivedAfternoteDetail,
            )
        }

        receiverComposable<ReceiverRoute.AfternoteDetailRoute> {
            ReceivedAfternoteDetailRoute(
                onNavigateBack = actions::popBack,
                onNavigateToFullList = actions::navigateToAfternoteList,
                onNavigateToPlaylist = actions::navigateToMemorialPlaylist,
            )
        }

        receiverComposable<ReceiverRoute.MemorialPlaylistRoute> {
            val playlistViewModel: ReceiverMemorialPlaylistViewModel = hiltViewModel()
            val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

            // 화면을 떠났다 돌아오면 다시 조회한다 — 백스택에 살아 있는 동안 옛 값이 남지 않게
            // 한다 (#701). 로딩을 방출하지 않는 refreshOnReturn() 을 쓰고, 최초 진입의 중복
            // 호출은 VM 이 진행 중인 Job 으로 막는다.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                playlistViewModel.refreshOnReturn()
            }
            when (val state = playlistUiState) {
                ReceiverMemorialPlaylistUiState.Loading -> {
                    LoadingBody()
                }

                is ReceiverMemorialPlaylistUiState.Error -> {
                    DetailLoadErrorContent(
                        messageRes = state.messageRes,
                        onBackClick = actions::popBack,
                        onRetryClick = playlistViewModel::retry,
                    )
                }

                is ReceiverMemorialPlaylistUiState.Success -> {
                    MemorialPlaylistScreen(
                        senderName = state.senderName,
                        songs = state.songs,
                        onBackClick = actions::popBack,
                    )
                }
            }
        }
    }
}

/** 열람 신청 단계가 동일한 그래프 범위의 ViewModel을 사용하도록 한다. */
@Composable
private fun NavBackStackEntry.deliveryFlowViewModel(deliveryFlowParentEntry: () -> NavBackStackEntry): DeliveryVerificationFlowViewModel {
    val parentEntry = remember(this) { deliveryFlowParentEntry() }
    return hiltViewModel(parentEntry)
}
