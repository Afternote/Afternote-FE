package com.afternote.feature.receiver.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.afternote.core.ui.navigation.FeatureNavDisplay
import com.afternote.core.ui.navigation.FeatureStackBoundary
import com.afternote.core.ui.navigation.replaceAllWith
import com.afternote.feature.receiver.presentation.invitation.ReceiverInvitationCompleteScreen
import com.afternote.feature.receiver.presentation.invitation.ReceiverInvitationLandingScreen
import com.afternote.feature.receiver.presentation.navigation.model.ReceiverInvitationRoute

/**
 * 초대 랜딩 로컬 스택이 셸에 남긴 이동 (#944).
 *
 * 스택 안의 랜딩 → 완료는 host 가 직접 처리하고, 셸이 답해야 하는 셋만 남는다.
 */
public interface ReceiverInvitationExternalActions {
    /** 로그인 전에 «수락» 을 눌렀다. 셸은 이 화면을 내리고 온보딩을 보이며, 로그인이 끝나면 다시 띄운다. */
    public fun requestLogin()

    /** 수락이 끝났거나 이미 등록된 수신자였다 — 받은 기록함으로. */
    public fun openReceivedRecords()

    /** «나중에 결정하기»·안내 확인 — 이 화면을 내린다. 토큰 처분은 ViewModel 이 이미 끝냈다. */
    public fun close()
}

/**
 * 카카오톡 초대 랜딩·완료의 로컬 Navigation 3 스택 (#944).
 *
 * @param isLoggedIn 셸이 판정한 인증 상태 — 랜딩의 «수락» 이 서버로 갈지 로그인으로 갈지 가른다.
 */
@Composable
public fun ReceiverInvitationNavHost(
    isLoggedIn: Boolean,
    boundary: FeatureStackBoundary,
    externalActions: ReceiverInvitationExternalActions,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(ReceiverInvitationRoute.LandingRoute)
    val actions = remember(backStack) { ReceiverInvitationLocalNavActions(backStack) }

    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<ReceiverInvitationRoute.LandingRoute> {
                    ReceiverTheme {
                        ReceiverInvitationLandingScreen(
                            isLoggedIn = isLoggedIn,
                            viewModel = hiltViewModel(),
                            onAccepted = actions::replaceLandingWithComplete,
                            onOpenReceivedRecords = externalActions::openReceivedRecords,
                            onLoginRequired = externalActions::requestLogin,
                            onClose = externalActions::close,
                        )
                    }
                }

                entry<ReceiverInvitationRoute.CompleteRoute> { key ->
                    ReceiverTheme {
                        ReceiverInvitationCompleteScreen(
                            inviterName = key.inviterName,
                            onConfirm = externalActions::openReceivedRecords,
                        )
                    }
                }
            },
    )
}
